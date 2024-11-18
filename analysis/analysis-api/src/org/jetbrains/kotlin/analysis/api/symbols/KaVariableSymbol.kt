/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
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

public sealed class KaVariableSymbol : KaCallableSymbol(), KaNamedSymbol {
    public abstract val isVal: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaVariableSymbol>
}

/**
 * Backing field of some member property
 *
 * E.g,
 * ```
 * val x: Int = 10
 *    get() = field<caret>
 * ```
 *
 * Symbol at caret will be resolved to a [KaBackingFieldSymbol]
 */
public abstract class KaBackingFieldSymbol : KaVariableSymbol() {
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
 * An entry of an enum class.
 *
 * The type of the enum entry is the enum class itself. The members declared in an enum entry's body are local to the body and cannot be
 * accessed from the outside. Hence, while it might look like enum entries can declare their own members (see the example below), they do
 * not have a (declared) member scope.
 *
 * Members declared by the enum class and overridden in the enum entry's body will be accessible, of course, but only the base version
 * declared in the enum class. For example, a narrowed return type of an overridden member in an enum entry's body will not be visible
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

    /**
     * Returns the enum entry's initializer, or `null` if the enum entry doesn't have a body.
     */
    public abstract val enumEntryInitializer: KaEnumEntryInitializerSymbol?

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
public interface KaEnumEntryInitializerSymbol : KaDeclarationContainerSymbol

public abstract class KaJavaFieldSymbol : KaVariableSymbol() {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isExpect: Boolean get() = withValidityAssertion { false }
    final override val isActual: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    public abstract val isStatic: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaJavaFieldSymbol>
}

@OptIn(KaImplementationDetail::class)
public sealed class KaPropertySymbol :
    KaVariableSymbol(),
    KaTypeParameterOwnerSymbol {

    /**
     * Checks if the property has a non-null [getter].
     *
     * Note: to check if the property has a **default** implicit getter, see [KaPropertyGetterSymbol.isDefault].
     */
    public abstract val hasGetter: Boolean

    /**
     * Checks if the property has a non-null [setter].
     *
     * Note: to check if the property has a default implicit setter, see [KaPropertySetterSymbol.isDefault].
     */
    public abstract val hasSetter: Boolean

    public abstract val getter: KaPropertyGetterSymbol?
    public abstract val setter: KaPropertySetterSymbol?
    public abstract val backingFieldSymbol: KaBackingFieldSymbol?

    public abstract val hasBackingField: Boolean

    public abstract val isDelegatedProperty: Boolean
    public abstract val isFromPrimaryConstructor: Boolean
    public abstract val isOverride: Boolean
    public abstract val isStatic: Boolean

    /**
     * Value which is provided for as property initializer.
     *
     * Possible values:
     * - `null` - no initializer was provided
     * - [KaConstantInitializerValue] - initializer value was provided, and it is a compile-time constant
     * - [KaNonConstantInitializerValue] - initializer value was provided, and it is not a compile-time constant. In case of declaration from source it would include correponding [KtExpression]
     *
     */
    @KaExperimentalApi
    public abstract val initializer: KaInitializerValue?

    abstract override fun createPointer(): KaSymbolPointer<KaPropertySymbol>
}

public abstract class KaKotlinPropertySymbol : KaPropertySymbol() {
    public abstract val isLateInit: Boolean

    public abstract val isConst: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol>
}

public abstract class KaSyntheticJavaPropertySymbol : KaPropertySymbol() {
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

    public abstract val javaGetterSymbol: KaNamedFunctionSymbol
    public abstract val javaSetterSymbol: KaNamedFunctionSymbol?

    abstract override fun createPointer(): KaSymbolPointer<KaSyntheticJavaPropertySymbol>
}

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

public abstract class KaValueParameterSymbol : KaParameterSymbol(), KaAnnotatedSymbol {
    /**
     * Returns true if the function parameter is marked with `noinline` modifier
     */
    public abstract val isNoinline: Boolean

    /**
     * Returns true if the function parameter is marked with `crossinline` modifier
     */
    public abstract val isCrossinline: Boolean

    /**
     * Whether this value parameter has a default value or not.
     */
    public abstract val hasDefaultValue: Boolean

    /**
     * Whether this value parameter represents a variable number of arguments (`vararg`) or not.
     */
    public abstract val isVararg: Boolean

    /**
     * Whether this value parameter is an implicitly generated lambda parameter `it` or not.
     */
    public abstract val isImplicitLambdaParameter: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol>

    /**
     * The name of the value parameter. For a parameter of `FunctionN.invoke()` functions, the name is taken from the function type
     * notation, if a name is present. For example:
     * ```
     * fun foo(x: (item: Int, String) -> Unit) =
     *   x(1, "") // or `x.invoke(1, "")`
     * ```
     * The names of the value parameters for `invoke()` are "item" and "p2" (its default parameter name).
     */
    abstract override val name: Name

    /**
     * The corresponding [KaPropertySymbol] if the current value parameter is a `val` or `var` declared inside the primary constructor.
     */
    public open val generatedPrimaryConstructorProperty: KaKotlinPropertySymbol? get() = null
}

/**
 * Symbol for a receiver parameter of a function or property. For example, consider code `fun String.foo() {...}`, the declaration of
 * `String` receiver parameter is such a symbol.
 */
public abstract class KaReceiverParameterSymbol : KaParameterSymbol() {
    /**
     * Link to the corresponding function or property.
     * In terms of the example above -- this is link to the function foo.
     */
    public abstract val owningCallableSymbol: KaCallableSymbol

    final override val name: Name
        get() = withValidityAssertion { SpecialNames.RECEIVER }

    abstract override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol>
}
