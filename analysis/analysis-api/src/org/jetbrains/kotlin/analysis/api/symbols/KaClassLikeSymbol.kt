/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaTypeParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

/**
 * [KaClassifierSymbol] represents a type declaration, including classes, objects, interfaces, type aliases, and type parameters.
 *
 * Typically, [KaClassifierSymbol] is not used directly. Instead, its inheritors should be used to access more specific information about a
 * declaration.
 *
 * @see KaClassLikeSymbol
 * @see KaTypeParameterSymbol
 */
public sealed class KaClassifierSymbol : KaDeclarationSymbol {
    public abstract val name: Name?

    abstract override fun createPointer(): KaSymbolPointer<KaClassifierSymbol>
}

/**
 * The name of the [KaClassifierSymbol], or [SpecialNames.ANONYMOUS] if it's an anonymous object.
 */
public val KaClassifierSymbol.nameOrAnonymous: Name
    get() = name ?: SpecialNames.ANONYMOUS

/**
 * [KaTypeParameterSymbol] represents a type parameter of a class, function, property, or type alias.
 */
public abstract class KaTypeParameterSymbol : KaClassifierSymbol(), KaNamedSymbol {
    /**
     * A list of [upper bounds](https://kotlinlang.org/docs/generics.html#upper-bounds) declared for the type parameter.
     *
     * #### Example
     *
     * ```
     * interface Bar
     *
     * class Foo<T : Bar>
     * ```
     *
     * The type parameter symbol for `T` has the upper bound `Bar`.
     */
    public abstract val upperBounds: List<KaType>

    /**
     * The type parameter's [declaration-site variance](https://kotlinlang.org/docs/generics.html#declaration-site-variance) (invariant,
     * covariant `out`, contravariant `in`).
     */
    public abstract val variance: Variance

    /**
     * Whether the type parameter has the `reified` modifier. `reified` is only applicable to type parameters of callables.
     */
    public abstract val isReified: Boolean

    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }

    abstract override fun createPointer(): KaSymbolPointer<KaTypeParameterSymbol>
}

/**
 * [KaClassLikeSymbol] represents a class, object, interface, or type alias declaration.
 *
 * @see KaClassifierSymbol
 * @see KaTypeAliasSymbol
 */
public sealed class KaClassLikeSymbol : KaClassifierSymbol() {
    /**
     * The fully-qualified [ClassId] of this class, or `null` if the class is local.
     */
    public abstract val classId: ClassId?

    abstract override fun createPointer(): KaSymbolPointer<KaClassLikeSymbol>
}

/**
 * [KaTypeAliasSymbol] represents a type alias declaration.
 */
@OptIn(KaImplementationDetail::class)
public abstract class KaTypeAliasSymbol : KaClassLikeSymbol(), KaNamedSymbol, KaTypeParameterOwnerSymbol {
    final override val modality: KaSymbolModality
        get() = withValidityAssertion { KaSymbolModality.FINAL }

    /**
     * The type this type alias expands to, which is the right-hand side of the `typealias` declaration. The type alias's [typeParameters]
     * will be contained in the resulting [KaType] unless they're unused.
     */
    public abstract val expandedType: KaType

    abstract override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol>
}

/**
 * [KaClassSymbol] represents a class, object, or interface declaration.
 *
 * Consider the following example use cases for [KaClassSymbol]:
 *
 *  - **Analyzing class hierarchies:** Use [superTypes] to navigate the inheritance hierarchy of a class and analyze its relationships with
 *  other classes and interfaces, or use the [isSubClassOf][org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider.isSubClassOf]
 *  function instead.
 *  - **Exploring class members:** Use scopes such as [declaredMemberScope][org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.declaredMemberScope]
 *  and [staticDeclaredMemberScope][org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.staticDeclaredMemberScope] to access the
 *  members of a class, including functions, properties, and nested classes. See [KaScopeProvider][org.jetbrains.kotlin.analysis.api.components.KaScopeProvider]
 *  for a comprehensive list of available scopes.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider.isSubClassOf
 * @see org.jetbrains.kotlin.analysis.api.components.KaScopeProvider
 */
public sealed class KaClassSymbol : KaClassLikeSymbol(), KaDeclarationContainerSymbol {
    /**
     * The kind of the class (e.g. ordinary class, interface, enum class, etc.).
     */
    public abstract val classKind: KaClassKind

    /**
     * A list of the direct supertypes. If the class has no explicit supertypes, the supertype will be [Any], or a special supertype such as
     * [Enum] for enum classes.
     *
     * Type parameters are included in supertype type arguments in an unsubstituted form. For example, if we have `class A<T> : B<T>`,
     * [superTypes] for `A` contains `B<T>` as a type, with an unsubstituted [KaTypeParameterType][org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType]
     * `T`.
     *
     * For a list of all supertypes, consider using [KaType.allSupertypes][org.jetbrains.kotlin.analysis.api.components.KaTypeProvider.allSupertypes]
     * on [KaNamedClassSymbol.defaultType][org.jetbrains.kotlin.analysis.api.components.KaTypeProvider.defaultType]. To check whether
     * this symbol is a subtype of another symbol, consider using [KaClassSymbol.isSubClassOf][org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider.isSubClassOf],
     * or [KaType.isSubtypeOf][org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.isSubtypeOf].
     */
    public abstract val superTypes: List<KaType>

    abstract override fun createPointer(): KaSymbolPointer<KaClassSymbol>
}

/**
 * [KaAnonymousObjectSymbol] represents anonymous object declarations.
 *
 * #### Example
 *
 * ```
 * val runnable = object : Runnable {
 *     override fun run() {}
 * }
 * ```
 */
public abstract class KaAnonymousObjectSymbol : KaClassSymbol() {
    final override val classKind: KaClassKind get() = withValidityAssertion { KaClassKind.ANONYMOUS_OBJECT }
    final override val classId: ClassId? get() = withValidityAssertion { null }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }

    final override val isExpect: Boolean get() = withValidityAssertion { false }
    final override val isActual: Boolean get() = withValidityAssertion { false }

    final override val name: Name? get() = withValidityAssertion { null }

    abstract override fun createPointer(): KaSymbolPointer<KaAnonymousObjectSymbol>
}

/**
 * [KaNamedClassSymbol] represents a named class, object, or interface declaration. The symbol covers most class declarations except for
 * anonymous objects, which are represented by [KaAnonymousObjectSymbol].
 */
@OptIn(KaImplementationDetail::class, KaExperimentalApi::class)
public abstract class KaNamedClassSymbol : KaClassSymbol(),
    KaTypeParameterOwnerSymbol,
    KaNamedSymbol,
    KaContextReceiversOwner {

    /**
     * Whether the class is an [inner class](https://kotlinlang.org/docs/nested-classes.html#inner-classes).
     */
    public abstract val isInner: Boolean

    /**
     * Whether the class is a [data class](https://kotlinlang.org/docs/data-classes.html).
     */
    public abstract val isData: Boolean

    /**
     * Whether the class is an [inline class](https://kotlinlang.org/docs/inline-classes.html).
     */
    public abstract val isInline: Boolean

    /**
     * Whether the class is a [functional interface](https://kotlinlang.org/docs/fun-interfaces.html).
     */
    public abstract val isFun: Boolean

    /**
     * Whether the class is implemented outside of Kotlin (accessible through [JNI](https://kotlinlang.org/docs/java-interop.html#using-jni-with-kotlin)
     * or [JavaScript](https://kotlinlang.org/docs/js-interop.html#external-modifier)).
     */
    public abstract val isExternal: Boolean

    /**
     * The nested companion object, or `null` if there is no companion object.
     */
    public abstract val companionObject: KaNamedClassSymbol?

    abstract override fun createPointer(): KaSymbolPointer<KaNamedClassSymbol>
}

/**
 * The kind of class represented by a [KaClassSymbol].
 */
public enum class KaClassKind {
    /**
     * A [regular class](https://kotlinlang.org/docs/classes.html) declaration:
     *
     * ```kotlin
     * class Person { }
     * ```
     *
     * @see KaNamedClassSymbol
     */
    CLASS,

    /**
     * An [enum class](https://kotlinlang.org/docs/enum-classes.html) declaration:
     *
     * ```kotlin
     * enum class Direction {
     *     NORTH, SOUTH, WEST, EAST
     * }
     * ```
     *
     * @see KaNamedClassSymbol
     */
    ENUM_CLASS,

    /**
     * An [annotation class](https://kotlinlang.org/docs/annotations.html) declaration:
     *
     * ```kotlin
     * annotation class Fancy
     * ```
     *
     * @see KaNamedClassSymbol
     */
    ANNOTATION_CLASS,

    /**
     * An [object](https://kotlinlang.org/docs/object-declarations.html#object-declarations-overview) declaration:
     *
     * ```kotlin
     * object Application { }
     * ```
     *
     * @see KaNamedClassSymbol
     */
    OBJECT,

    /**
     * A [companion object](https://kotlinlang.org/docs/object-declarations.html#companion-objects) declaration:
     *
     * ```
     * class MyClass {
     *     companion object Factory {
     *         fun create(): MyClass = MyClass()
     *     }
     * }
     * ```
     *
     * @see KaNamedClassSymbol
     */
    COMPANION_OBJECT,

    /**
     * An [interface](https://kotlinlang.org/docs/interfaces.html) declaration:
     *
     * ```kotlin
     * interface Named {
     *     val name: String
     * }
     * ```
     *
     * @see KaNamedClassSymbol
     */
    INTERFACE,

    /**
     * An [anonymous object](https://kotlinlang.org/docs/object-declarations.html#object-expressions):
     *
     * ```kotlin
     * val runnable = object : Runnable {
     *  *     override fun run() {}
     *  * }
     * ```
     *
     * @see KaAnonymousObjectSymbol
     */
    ANONYMOUS_OBJECT;

    public val isObject: Boolean get() = this == OBJECT || this == COMPANION_OBJECT || this == ANONYMOUS_OBJECT
    public val isClass: Boolean get() = this == CLASS || this == ANNOTATION_CLASS || this == ENUM_CLASS
}
