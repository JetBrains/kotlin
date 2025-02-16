/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaContextParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaTypeParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * [KaVariableSymbol] represents a variable-like declaration, including properties, local variables, and value parameters.
 */
public sealed class KaVariableSymbol : KaCallableSymbol(), KaNamedSymbol {
    /**
     * Whether the declaration is read-only.
     */
    public abstract val isVal: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaVariableSymbol>
}

/**
 * [KaBackingFieldSymbol] represents the [backing field](https://kotlinlang.org/docs/properties.html#backing-fields) of a property.
 *
 * #### Example
 *
 * ```kotlin
 * val x: Int = 10
 *     get() = field
 * ```
 *
 * The symbol for `field` is a [KaBackingFieldSymbol].
 *
 * @see KaPropertySymbol.backingFieldSymbol
 */
public abstract class KaBackingFieldSymbol : KaVariableSymbol() {
    /**
     * The property which is backed by the backing field.
     */
    public abstract val owningProperty: KaKotlinPropertySymbol

    final override val name: Name get() = withValidityAssertion { StandardNames.BACKING_FIELD }

    /** PSI may be not-null in the case of explicit backing field ([KEEP-278](https://github.com/Kotlin/KEEP/issues/278)) */
    final override val psi: PsiElement? get() = withValidityAssertion { null }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.PROPERTY }
    override val origin: KaSymbolOrigin get() = withValidityAssertion { KaSymbolOrigin.PROPERTY_BACKING_FIELD }
    final override val callableId: CallableId? get() = withValidityAssertion { null }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    // KT-70767: for the backing field expect/action is meaningless as it doesn't have such a semantic

    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Private }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    abstract override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol>
}

/**
 * [KaEnumEntrySymbol] represents an [enum entry declaration](https://kotlinlang.org/docs/enum-classes.html).
 *
 * In the Kotlin PSI, a [KtEnumEntry][org.jetbrains.kotlin.psi.KtEnumEntry] is a [KtClass][org.jetbrains.kotlin.psi.KtClass], which aligns
 * with the old K1 compiler. In the Analysis API, though, similarly to the K2 compiler, an enum entry is a [KaVariableSymbol].
 *
 * ### Enum entry type & members
 *
 * The type of the enum entry is the enum class itself. The members declared in an enum entry's body are local to the body and cannot be
 * accessed from the outside. Hence, while it might look like enum entries can declare their own members (see the example below), they do
 * not have a (declared) member scope.
 *
 * Members declared by the enum class and overridden in the enum entry's body will be accessible, of course, but only the base version
 * declared in the enum class. For example, the narrowed return type of an overridden member in an enum entry's body will not be visible
 * outside the body.
 *
 * #### Example
 *
 * ```kotlin
 * enum class E {
 *     A {
 *         val x: Int = 5
 *     }
 * }
 * ```
 *
 * `A` is an enum entry of enum class `E`. `x` is a property of `A`'s initializer and thus not accessible outside the initializer.
 */
public abstract class KaEnumEntrySymbol : KaVariableSymbol() {
    /**
     * The enum entry's initializer, or `null` if the enum entry doesn't have a body.
     */
    public abstract val enumEntryInitializer: KaEnumEntryInitializerSymbol?

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }
    final override val isVal: Boolean get() = withValidityAssertion { true }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Public }

    final override val isActual: Boolean get() = withValidityAssertion { false }

    abstract override fun createPointer(): KaSymbolPointer<KaEnumEntrySymbol>
}

/**
 * An initializer for enum entries with a body. The initializer may contain its own declarations (especially overrides of members declared
 * by the enum class), and is [similar to an object declaration](https://kotlinlang.org/spec/declarations.html#enum-class-declaration).
 *
 * #### Example
 *
 * ```kotlin
 * enum class E {
 *     // `A` is declared with an initializer.
 *     A {
 *         val x: Int = 5
 *     },
 *
 *     // `B` has no initializer.
 *     B
 * }
 * ```
 *
 * The initializer of `A` declares a member `x: Int`, which is inaccessible outside the initializer. Still, the corresponding
 * [KaEnumEntryInitializerSymbol] can be used to get a declared member scope that contains `x`.
 */
public interface KaEnumEntryInitializerSymbol : KaDeclarationContainerSymbol {
    override fun createPointer(): KaSymbolPointer<KaEnumEntryInitializerSymbol>
}

/**
 * [KaJavaFieldSymbol] represents a [Java field declaration](https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.3).
 */
public abstract class KaJavaFieldSymbol : KaVariableSymbol() {
    /**
     * Whether the Java field is [static](https://docs.oracle.com/javase/specs/jls/se23/html/jls-8.html#jls-8.3.1.1).
     */
    public abstract val isStatic: Boolean

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isExpect: Boolean get() = withValidityAssertion { false }
    final override val isActual: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    abstract override fun createPointer(): KaSymbolPointer<KaJavaFieldSymbol>
}

/**
 * [KaPropertySymbol] represents a [property declaration](https://kotlinlang.org/docs/properties.html).
 */
@OptIn(KaImplementationDetail::class)
public sealed class KaPropertySymbol : KaVariableSymbol(), KaTypeParameterOwnerSymbol {
    /**
     * Whether the property has a non-null [getter].
     *
     * To check if the property's getter is a **default** implicit getter, see [KaPropertyGetterSymbol.isDefault].
     */
    public abstract val hasGetter: Boolean

    /**
     * Whether the property has a non-null [setter].
     *
     * To check if the property's setter is a **default** implicit setter, see [KaPropertySetterSymbol.isDefault].
     */
    public abstract val hasSetter: Boolean

    /**
     * The property's explicit or default [getter](https://kotlinlang.org/docs/properties.html#getters-and-setters).
     */
    public abstract val getter: KaPropertyGetterSymbol?

    /**
     * The property's explicit or default [setter](https://kotlinlang.org/docs/properties.html#getters-and-setters).
     */
    public abstract val setter: KaPropertySetterSymbol?

    /**
     * Whether the property has a non-null [backingFieldSymbol].
     */
    public abstract val hasBackingField: Boolean

    /**
     * The property's [backing field](https://kotlinlang.org/docs/properties.html#backing-fields), if the property has one.
     */
    public abstract val backingFieldSymbol: KaBackingFieldSymbol?

    /**
     * Whether the property is a [delegated property](https://kotlinlang.org/docs/delegated-properties.html).
     */
    public abstract val isDelegatedProperty: Boolean

    /**
     * Whether the property is declared in a class's primary constructor.
     *
     * Properties may be declared directly in the primary constructor of a class. The compiler generates a property from such a declaration,
     * which is initialized with the argument passed to the corresponding primary constructor parameter.
     *
     * #### Example
     *
     * ```kotlin
     * class Foo(val name: String) {
     *     val count: Int = 5
     * }
     * ```
     *
     * `Foo.name` is declared in `Foo`'s primary constructor. The compiler generates a corresponding property which is accessible via the
     * class's [member scope][org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.memberScope], as well as the primary
     * constructor's value parameters via [KaValueParameterSymbol.generatedPrimaryConstructorProperty].
     *
     * In contrast, `Foo.count` is not declared in the primary constructor.
     */
    public abstract val isFromPrimaryConstructor: Boolean

    /**
     * Whether the property is an [override property](https://kotlinlang.org/docs/inheritance.html#overriding-properties).
     */
    public abstract val isOverride: Boolean

    /**
     * Whether the property is static. While Kotlin properties cannot be static, the property symbol may represent e.g. a static Java field.
     */
    public abstract val isStatic: Boolean

    /**
     * Whether the property is implemented outside of Kotlin (accessible through [JNI](https://kotlinlang.org/docs/java-interop.html#using-jni-with-kotlin)
     * or [JavaScript](https://kotlinlang.org/docs/js-interop.html#external-modifier)).
     */
    public abstract val isExternal: Boolean

    /**
     * The value which is used as the property's initializer.
     *
     * Possible cases are:
     *
     * - `null` - the property doesn't have an initializer.
     * - [KaConstantInitializerValue][org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue] - the property has an initializer with a
     *   compile-time constant value.
     * - [KaNonConstantInitializerValue][org.jetbrains.kotlin.analysis.api.KaNonConstantInitializerValue] - the property has an initializer
     *   with a non-constant value. If the initializer is declared in sources, the value includes the corresponding
     *   [KtExpression][org.jetbrains.kotlin.psi.KtExpression].
     * - [KaConstantValueForAnnotation][org.jetbrains.kotlin.analysis.api.KaConstantValueForAnnotation] - the property is contained in an
     *   annotation class and has an initializer which can be evaluated to a
     *   [KaAnnotationValue][org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue].
     */
    @KaExperimentalApi
    public abstract val initializer: KaInitializerValue?

    abstract override fun createPointer(): KaSymbolPointer<KaPropertySymbol>
}

/**
 * [KaKotlinPropertySymbol] represents a *Kotlin* property symbol, in contrast to [KaSyntheticJavaPropertySymbol].
 */
@OptIn(KaExperimentalApi::class, KaImplementationDetail::class)
public abstract class KaKotlinPropertySymbol : KaPropertySymbol(), KaContextParameterOwnerSymbol {
    /**
     * Whether the property is a [late-initialized property](https://kotlinlang.org/docs/properties.html#late-initialized-properties-and-variables).
     */
    public abstract val isLateInit: Boolean

    /**
     * Whether the property is a [compile-time constant](https://kotlinlang.org/docs/properties.html#compile-time-constants).
     */
    public abstract val isConst: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol>
}

/**
 * [KaSyntheticJavaPropertySymbol] represents a synthetic property generated by the compiler for a Java field associated with a getter or
 * setter. This allows Java fields to be accessed like Kotlin properties.
 *
 * #### Example
 *
 * ```java
 * public class JavaClass {
 *     private int field;
 *
 *     public int getField() {
 *         return field;
 *     }
 *
 *     public void setField(int field) {
 *         this.field = field;
 *     }
 * }
 * ```
 *
 * The compiler generates a synthetic property `field` with a getter and setter based on the Java methods `getField()` and `setField()`.
 *
 * @see KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY
 */
public abstract class KaSyntheticJavaPropertySymbol : KaPropertySymbol() {
    /**
     * The function symbol for the original Java getter method.
     *
     * #### Example
     *
     * ```java
     * public class JavaClass {
     *     private int field;
     *
     *     public int getField() {
     *         return field;
     *     }
     * }
     * ```
     *
     * In the synthetic property for `field`, [javaGetterSymbol] is the function symbol for `getField`.
     */
    public abstract val javaGetterSymbol: KaNamedFunctionSymbol

    /**
     * The function symbol for the original Java setter method, if it exists.
     *
     * #### Example
     *
     * ```java
     * public class JavaClass {
     *     private int field;
     *
     *     public int getField() {
     *         return field;
     *     }
     *
     *     public void setField(int field) {
     *         this.field = field;
     *     }
     * }
     * ```
     *
     * In the synthetic property for `field`, [javaSetterSymbol] is the function symbol for `setField`.
     */
    public abstract val javaSetterSymbol: KaNamedFunctionSymbol?

    final override val hasBackingField: Boolean get() = withValidityAssertion { true }
    final override val isDelegatedProperty: Boolean get() = withValidityAssertion { false }
    final override val hasGetter: Boolean get() = withValidityAssertion { true }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }
    final override val backingFieldSymbol: KaBackingFieldSymbol? get() = withValidityAssertion { null }
    final override val isFromPrimaryConstructor: Boolean get() = withValidityAssertion { false }
    override val origin: KaSymbolOrigin get() = withValidityAssertion { KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    abstract override val getter: KaPropertyGetterSymbol

    abstract override fun createPointer(): KaSymbolPointer<KaSyntheticJavaPropertySymbol>
}

/**
 * [KaLocalVariableSymbol] represents a local variable.
 */
public abstract class KaLocalVariableSymbol : KaVariableSymbol() {
    final override val callableId: CallableId? get() = withValidityAssertion { null }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }

    abstract override fun createPointer(): KaSymbolPointer<KaLocalVariableSymbol>
}

/**
 * [KaParameterSymbol] represents a value parameter, context parameter, or receiver parameter.
 *
 * @see KaValueParameterSymbol
 * @see KaReceiverParameterSymbol
 * @see KaContextParameterSymbol
 */
public sealed class KaParameterSymbol : KaVariableSymbol() {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }

    final override val callableId: CallableId? get() = withValidityAssertion { null }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }
    final override val isVal: Boolean get() = withValidityAssertion { true }
    final override val isExpect: Boolean get() = withValidityAssertion { false }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    abstract override fun createPointer(): KaSymbolPointer<KaParameterSymbol>
}

/**
 * [KaContextParameterSymbol] represents a context parameter of a [KaNamedFunctionSymbol], [KaAnonymousFunctionSymbol], or [KaKotlinPropertySymbol].
 *
 * See [KEEP-367](https://github.com/Kotlin/KEEP/issues/367) for more details.
 *
 * #### Example
 *
 * ```kotlin
 * context(stringContext: String)
 * fun foo() { ... }
 * ```
 *
 * The `stringContext` context parameter of `foo` would be represented by [KaContextParameterSymbol].
 *
 * @see KaCallableSymbol.contextParameters
 */
@KaExperimentalApi
public abstract class KaContextParameterSymbol : KaParameterSymbol() {
    abstract override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol>
}

/**
 * [KaValueParameterSymbol] represents a value parameter of a function, constructor, or property setter.
 *
 * In Kotlin, we generally use the phrase "value parameter," as functions have different kinds of parameters, such as value, receiver,
 * context, and type parameters.
 *
 * @see KaFunctionSymbol.valueParameters
 */
public abstract class KaValueParameterSymbol : KaParameterSymbol() {
    /**
     * The name of the value parameter.
     *
     * For a parameter of `FunctionN.invoke()` functions, the name is taken from the function type notation, if a name is present. For
     * example:
     *
     * ```kotlin
     * fun foo(x: (item: Int, String) -> Unit) =
     *     x(1, "") // or `x.invoke(1, "")`
     * ```
     *
     * The names of the value parameters for `invoke()` are "item" and "p2" (its default parameter name).
     */
    abstract override val name: Name

    /**
     * Whether the value parameter is marked as [`noinline`](https://kotlinlang.org/docs/inline-functions.html#noinline).
     */
    public abstract val isNoinline: Boolean

    /**
     * Whether the value parameter is marked as [`crossinline`](https://kotlinlang.org/docs/inline-functions.html#non-local-returns).
     */
    public abstract val isCrossinline: Boolean

    /**
     * Whether the value parameter has a [default value](https://kotlinlang.org/docs/functions.html#default-arguments).
     */
    public abstract val hasDefaultValue: Boolean

    /**
     * Whether the value parameter represents a [variable number of arguments (`vararg`)](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs).
     */
    public abstract val isVararg: Boolean

    /**
     * Whether the value parameter is an implicitly generated lambda parameter (`it`).
     */
    public abstract val isImplicitLambdaParameter: Boolean

    /**
     * The associated generated [KaPropertySymbol] if this value parameter corresponds to a `val` or `var` property declaration in a primary
     * constructor.
     *
     * @see KaPropertySymbol.isFromPrimaryConstructor
     */
    public open val generatedPrimaryConstructorProperty: KaKotlinPropertySymbol? get() = null

    abstract override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol>
}

/**
 * A symbol for a receiver parameter of an [extension function or property](https://kotlinlang.org/docs/extensions.html).
 *
 * #### Example
 *
 * ```kotlin
 * fun String.foo() { ... }
 * ```
 *
 * The `String` receiver parameter of `foo` would be represented by [KaReceiverParameterSymbol].
 */
public abstract class KaReceiverParameterSymbol : KaParameterSymbol() {
    /**
     * The corresponding function or property in which the receiver parameter is declared.
     *
     * #### Example
     *
     * ```kotlin
     * fun String.foo() { ... }
     * ```
     *
     * For the `String` receiver parameter, [owningCallableSymbol] is `foo`.
     */
    public abstract val owningCallableSymbol: KaCallableSymbol

    final override val name: Name
        get() = withValidityAssertion { SpecialNames.RECEIVER }

    abstract override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol>
}
