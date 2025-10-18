/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * [KaTypePointer] allows to point to a [KaType] and later retrieve it in another [KaSession]. A pointer is necessary because [KaType]s
 * cannot be shared past the boundaries of the [KaSession] they were created in, as they are valid only there.
 *
 * @see KaSymbolPointer
 * @see org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypePointer<out T : KaType> {
    /**
     * Returns the restored [KaType] (possibly a new type instance) if the pointer is still valid, or `null` otherwise.
     *
     * Do not use this function directly, as it is an implementation detail.
     * Use [KaSession.restore][org.jetbrains.kotlin.analysis.api.KaSession.restore] instead.
     */
    @KaImplementationDetail
    public fun restore(session: KaSession): T?
}

/**
 * [KaType] represents a concrete Kotlin type, such as `Int`, `Foo` for a class `Foo`, or `Bar<String>` for a class `Bar<T>`. It provides
 * information about type structure, nullability, and annotations.
 *
 * The represented type may either be valid, or a [KaErrorType]. In that case, [KaErrorType] and the more specific [KaClassErrorType]
 * provide additional information about the nature of the type error, such as an [error message][KaErrorType.errorMessage].
 *
 * ### Structural and semantic equality
 *
 * [KaType.equals] implements *structural type equality*, which may not match with the usual intuition of type equality. Structural equality
 * is favored for `equals` because it is fast and predictable, and additionally it allows constructing a hash code. For semantic type
 * comparisons, [semanticallyEquals][org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.semanticallyEquals] should be used,
 * as it implements the equality rules defined by the type system.
 *
 * While structural equality lends itself well to usage of [KaType] as a key, avoid relying on hash maps to collect equal [KaType]s, as you
 * most likely want semantic equality in such cases. A possible alternative would be to use a hash map, but with a post-processing step of
 * comparing the map's keys with [semanticallyEquals][org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker.semanticallyEquals]
 * to uncover additional equal types.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeProvider
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeCreator
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaType : KaLifetimeOwner, KaAnnotated {
    /**
     * The type's [nullability][KaTypeNullability].
     *
     * [KaTypeNullability] was deprecated due to its questionable meaning, see KT-71101 for more information.
     *
     * Now the information about the nullability can be acquired via three properties:
     *
     * ##### 1. [org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isMarkedNullable]
     * Shows whether some type is marked as nullable, i.e., is represented as `T?` in code. Previously, [nullability] had the same semantics.
     *
     * ##### 2. [org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isNullable]
     * Shows whether some type can hold `null` value. It unwraps type aliases, checks parameter type bounds, etc. and performs a deep nullability calculation.
     *
     * Note the difference between `isMarkedNullable` and `isNullable`. `isMarkedNullable = false` doesn't imply that this type cannot hold `null`. Consider the following example:
     * ```kotlin
     * fun <T : String?> foo(something: T) {}
     * ```
     *
     * `isMarkedNullable` for type `T` returns `false`, as it's not marked as nullable. However, it still can hold `null`, as its upper bound is nullable, so `isNullable` is `true`.
     *
     * ##### 3. [org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.hasFlexibleNullability]
     * Shows whether some type has flexible nullability, i.e., both null-safe and non-null-safe calls are valid on this type.
     * Such types are error types with unknown nullability or flexible / dynamic types with a non-nullable lower bound and a nullable upper bound. Previously, [nullability] in such cases was [KaTypeNullability.UNKNOWN].
     *
     * Note that `isMarkedNullable` for flexible / dynamic types returns `true` only if both of its bounds are marked as nullable, otherwise both bounds can either be non-nullable or the type can have flexible nullability.
     * The same is applied to error types: `isMarkedNullable` returns `true` only when this error type is definitely nullable, otherwise it can either be non-nullable or have unknown nullability.
     */
    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    public val nullability: KaTypeNullability

    /**
     * The abbreviated type for this expanded [KaType], or `null` if this type has not been expanded from an abbreviated type or the
     * abbreviated type cannot be resolved.
     *
     * An abbreviated type is a type alias application that has been expanded to some other Kotlin type. For example, if we have a type
     * alias `typealias MyString = String` and its application `MyString`, `String` would be the type alias expansion and `MyString` its
     * abbreviated type.
     *
     * The abbreviated type contains the type arguments of a specific type alias application. For example, if we have a
     * `typealias MyList<A> = List<A>`, for an application `MyList<String>`, `MyList<String>` would be the abbreviated type for such a type
     * alias application, not simply `MyList`.
     *
     * If this [KaType] is an unexpanded type alias application, [abbreviation] is `null`. Not all type alias applications are currently
     * expanded right away and the Analysis API makes no guarantees about the specific circumstances.
     *
     * While [abbreviation] is available for all [KaType]s, it can currently only be present in [KaClassType]s. However, abbreviated
     * types are a general concept and if the type system changes (e.g. with denotable union/intersection types), other kinds of types may
     * also be expanded from a type alias. This would allow more kinds of types to carry an abbreviated type.
     *
     * The [abbreviation] itself is always a [KaUsualClassType], as the application of a type alias is always a class type. It cannot be
     * a [KaClassErrorType] because [abbreviation] would then be `null`.
     *
     *
     * ### Resolvability
     *
     * Even when this [KaType] is an expansion, the abbreviated type may be `null` if it is not resolvable from this type's use-site module.
     * This can occur when the abbreviated type from a module `M1` was expanded at some declaration `D` in module `M2`, and the use-site
     * module uses `D`, but only has a dependency on `M2`. Then the type alias of `M1` remains unresolved and [abbreviation] is `null`.
     *
     *
     * ### Type arguments and nested abbreviated types
     *
     * The type arguments of an abbreviated type are not converted to abbreviated types automatically. That is, if a type argument is a type
     * expansion, its [abbreviation] doesn't automatically replace the expanded type. For example:
     *
     * ```
     * typealias MyString = String
     * typealias MyList<A> = List<A>
     *
     * val list: MyList<MyString> = listOf()
     * ```
     *
     * `MyList<MyString>` may be expanded to a type `List<String>` with an abbreviated type `MyList<String>`, where `String` also has the
     * abbreviated type `MyString`. The abbreviated type is not `MyList<MyString>`, although it might be rendered as such.
     *
     *
     * ### Transitive expansion
     *
     * Types are always expanded to their final form. That is, if we have a chain of type alias expansions, the [KaType] only represents the
     * final expanded type, and its [abbreviation] the initial type alias application. For example:
     *
     * ```
     * typealias Inner = String
     * typealias Outer = Inner
     *
     * val outer: Outer = ""
     * ```
     *
     * Here, `outer`'s type would be expanded to `String`, but its abbreviated type would be `Outer`. `Inner` would be lost.
     */
    public val abbreviation: KaUsualClassType?

    /**
     * Creates a type pointer.
     *
     * Unlike [KaType], a [KaTypePointer] may be safely stored and passed around outside the
     * (analyze)[org.jetbrains.kotlin.analysis.api.analyze] block. Use the [KaSession.restore] function to get the type instance back.
     * Note that depending on the use-site session (analysisScope)[KaSession.analysisScope], a type might not be restored.
     */
    @KaExperimentalApi
    @KaK1Unsupported
    public fun createPointer(): KaTypePointer<KaType>
}

/**
 * The [nullability](https://kotlinlang.org/docs/null-safety.html#nullable-types-and-non-nullable-types) of a [KaType].
 */
@Deprecated("See KDocs for `KaType.nullability` for the migration guide")
public enum class KaTypeNullability(public val isNullable: Boolean) {
    /**
     * The [KaType] is nullable, i.e. it can hold `null`.
     */
    NULLABLE(true),

    /**
     * The [KaType] is not nullable, i.e. it cannot hold `null`.
     */
    NON_NULLABLE(false),

    /**
     * The [KaType]'s nullability is not known, for example in some [flexible types][KaFlexibleType] and some [error types][KaErrorType].
     */
    UNKNOWN(false);

    public companion object {
        @Suppress("Deprecation")
        public fun create(isNullable: Boolean): KaTypeNullability = if (isNullable) NULLABLE else NON_NULLABLE
    }
}

/**
 * [KaErrorType] represents a type that failed to resolve correctly.
 *
 * The more specific [KaClassErrorType] has additional information available to work with.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaErrorType : KaType {
    @KaNonPublicApi
    public val errorMessage: String

    @KaNonPublicApi
    public val presentableText: String?

    @KaExperimentalApi
    public override fun createPointer(): KaTypePointer<KaErrorType>
}

/**
 * [KaClassType] represents a generic class type or a function type.
 *
 * In Kotlin, function types are class types. This is why the Analysis API differentiates between [function types][KaFunctionType] and
 * [*usual* class types][KaUsualClassType], which encompass all non-function class types.
 */
@OptIn(KaImplementationDetail::class)
public sealed class KaClassType : KaType {
    /**
     * The [ClassId] of the class.
     */
    public abstract val classId: ClassId

    /**
     * The class symbol which this class type is an instance of.
     */
    public abstract val symbol: KaClassLikeSymbol

    /**
     * The type arguments of the class type.
     *
     * Type arguments should not be confused with the [symbol]'s type parameters (for some subtypes of [KaClassLikeSymbol]).
     */
    public abstract val typeArguments: List<KaTypeProjection>

    /**
     * The list of [KaResolvedClassTypeQualifier]s describing the segments of the class type.
     *
     * @see KaClassTypeQualifier
     */
    public abstract val qualifiers: List<KaResolvedClassTypeQualifier>

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaClassType>
}

/**
 * [KaFunctionType] represents a Kotlin [function type](https://kotlinlang.org/docs/lambdas.html#function-types), such as `(String) -> Int`
 * or `suspend () -> List<Any>`.
 */
@OptIn(KaExperimentalApi::class)
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaFunctionType : KaClassType(), KaContextReceiversOwner {
    /**
     * The [extension receiver](https://kotlinlang.org/docs/extensions.html) type, or `null` if the function type is not an extension
     * function type.
     *
     * #### Example
     *
     * ```kotlin
     * Foo.(Bar, String, String) -> Int
     * ```
     *
     * The function type above has the following receiver type: `Foo`.
     */
    public abstract val receiverType: KaType?

    /**
     * Whether the function type is an extension function type.
     *
     * @see receiverType
     */
    public abstract val hasReceiver: Boolean

    /**
     * The function's parameter types, *excluding* receiver types and context receivers.
     *
     * This should not be confused with [typeArguments], which also include the function's return type, or the [symbol]'s type parameters.
     *
     * #### Example
     *
     * ```kotlin
     * Foo.(Bar, String, String) -> Int
     * ```
     *
     * The function type above has the following parameter types: `Bar`, `String`, `String`.
     */
    public abstract val parameterTypes: List<KaType>

    /**
     * The function's value parameters, *excluding* receiver types and context receivers.
     *
     * [parameters] represent the same list of parameters as [parameterTypes] along with their names, if available.
     *
     * #### Example
     *
     * ```kotlin
     * Foo.(x: Bar, y: String, z: String) -> Int
     * ```
     *
     * The function type above has the following value parameters: `x`, `y`, `z`.
     */
    @KaExperimentalApi
    public abstract val parameters: List<KaFunctionValueParameter>

    /**
     * The function's arity, i.e. the number of [*parameter types*][parameterTypes].
     */
    @Deprecated("Use `parameters.size` instead. See KT-80545", ReplaceWith("parameters.size"))
    public abstract val arity: Int

    /**
     * The function's return type.
     *
     * #### Example
     *
     * ```kotlin
     * Foo.(Bar, String, String) -> Int
     * ```
     *
     * The function type above has the following return type: `Int`.
     */
    public abstract val returnType: KaType

    /**
     * Whether the function is a [suspend function](https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html#suspending-functions).
     */
    public abstract val isSuspend: Boolean

    /**
     * Whether the type is a [reflection function type](https://kotlinlang.org/docs/reflection.html#function-references).
     */
    public abstract val isReflectType: Boolean

    /**
     * Whether the function type has context receiver parameters.
     */
    @KaExperimentalApi
    public abstract val hasContextReceivers: Boolean

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaFunctionType>
}


/**
 * Represents a function value parameter
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaFunctionValueParameter : KaLifetimeOwner {
    /**
     * Type of the parameter
     */
    public abstract val type: KaType

    /**
     * Name of the parameter, as specified in the function signature.
     *
     * Note that some parameters might not have names by their nature.
     * This applies to, for example, receiver parameters in references to member functions.
     *
     * ```kotlin
     * class A {
     *     fun foo(x: Int) {}
     * }
     *
     * val fooRef = A::foo
     * ```
     *
     * `fooRef` from the example above has `KaFunctionType`.
     * This type has two `KaFunctionValueParameter`s:
     *  - One receiver parameter of type `A`, which doesn't have any specified name
     *  - One regular parameter `x` of type `Int`
     */
    public abstract val name: Name?
}

/**
 * [KaUsualClassType] represents a generic class type, such as `String` or `List<Int>`.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaUsualClassType : KaClassType() {
    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaUsualClassType>
}

/**
 * [KaClassErrorType] represents a class type that failed to resolve correctly.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaClassErrorType : KaErrorType {
    /**
     * The list of [KaClassTypeQualifier]s describing the segments of the class error type.
     *
     * Depending on the kind of error, some or all qualifiers may be [KaResolvedClassTypeQualifier]s, which allow retrieving additional
     * information about the type despite the error. For example, a type error may be "invalid number of type arguments," but the qualifier
     * will be resolved regardless.
     */
    public abstract val qualifiers: List<KaClassTypeQualifier>

    /**
     * A list of candidate class symbols that were considered when resolving the type, despite the ultimate type error.
     */
    public abstract val candidateSymbols: Collection<KaClassLikeSymbol>

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaClassErrorType>
}

/**
 * [KaTypeParameterType] represents a type parameter type, such as `T` in the declaration `class Box<T>(val element: T)`.
 *
 * In that sense, [KaTypeParameterType] is a type used in *unsubstituted* positions to represent an application of a type parameter.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaTypeParameterType : KaType {
    /**
     * The type parameter's simple name.
     */
    public abstract val name: Name

    /**
     * The [KaTypeParameterSymbol] which this type is an instance of.
     */
    public abstract val symbol: KaTypeParameterSymbol

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaTypeParameterType>
}

/**
 * [KaCapturedType] represents a [captured type](https://kotlinlang.org/spec/type-system.html#type-capturing).
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaCapturedType : KaType {
    /**
     * The source type argument of the captured type.
     */
    public abstract val projection: KaTypeProjection

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaCapturedType>
}

/**
 * [KaDefinitelyNotNullType] represents a [definitely not-null type](https://kotlinlang.org/docs/generics.html#definitely-non-nullable-types),
 * such as `T & Any` for a type parameter `T`.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaDefinitelyNotNullType : KaType {
    /**
     * The nullable upper bound of the type.
     */
    public abstract val original: KaType

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    final override val nullability: KaTypeNullability get() = withValidityAssertion { KaTypeNullability.NON_NULLABLE }

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaDefinitelyNotNullType>
}

/**
 * [KaFlexibleType] represents a [flexible type](https://kotlinlang.org/spec/type-system.html#flexible-types) (or a so-called
 * [platform type](https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types)), a range of types from the [lowerBound] to
 * the [upperBound] (both inclusive).
 *
 * A flexible type's [abbreviation] is always `null`, as only [lowerBound] and [upperBound] can actually be expanded types.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaFlexibleType : KaType {
    /**
     * The lower bound, such as `String` in `String!`.
     */
    public abstract val lowerBound: KaType

    /**
     * The upper bound, such as `String?` in `String!`.
     */
    public abstract val upperBound: KaType

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaFlexibleType>
}

/**
 * [KaIntersectionType] represents an [intersection type](https://kotlinlang.org/spec/type-system.html#intersection-types), such as `A & B`.
 * Intersection types cannot be denoted in Kotlin code, but can result from some compiler operations, such as
 * [smart casts](https://kotlinlang.org/spec/type-inference.html#smart-casts).
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaIntersectionType : KaType {
    /**
     * A list of individual types participating in the intersection.
     */
    public abstract val conjuncts: List<KaType>

    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaIntersectionType>
}

/**
 * [KaDynamicType] represents a [dynamic type](https://kotlinlang.org/docs/dynamic-type.html), which is used to support interoperability
 * with dynamically typed libraries, platforms, or languages.
 *
 * Although this can be viewed as a flexible type (`kotlin.Nothing..kotlin.Any?`), a platform may assign special meaning to the values of a
 * dynamic type, and handle it differently from the regular flexible type.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaDynamicType : KaType {
    @KaExperimentalApi
    @KaK1Unsupported
    public abstract override fun createPointer(): KaTypePointer<KaDynamicType>
}
