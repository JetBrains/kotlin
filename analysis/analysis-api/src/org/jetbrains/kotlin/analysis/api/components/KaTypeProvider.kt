/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

public interface KaTypeProvider : KaSessionComponent {
    /**
     * [builtinTypes] provides [KaType] instances for built-in types.
     */
    public val builtinTypes: KaBuiltinTypes

    /**
     * Approximates [KaType] to a [denotable][KaTypeInformationProvider.isDenotable] supertype.
     *
     * The function returns `null` if the type is already denotable and does not need approximation. Otherwise, for a type `T`, returns a
     * denotable type `S` such that `T <: S`, with all type arguments of `S` also being denotable.
     *
     * @param approximateLocalTypes Whether locally declared types should be approximated to non-local supertypes. Avoiding local type
     *  approximation is sensible when the resulting [KaType] is analyzed in the same local context.
     */
    @KaExperimentalApi
    public fun KaType.approximateToSuperPublicDenotable(approximateLocalTypes: Boolean): KaType?

    /**
     * Approximates [KaType] to a [denotable][KaTypeInformationProvider.isDenotable] supertype, or returns the given type itself if it is
     * already denotable.
     *
     * @see approximateToSuperPublicDenotable
     */
    @KaExperimentalApi
    public fun KaType.approximateToSuperPublicDenotableOrSelf(approximateLocalTypes: Boolean): KaType = withValidityAssertion {
        return approximateToSuperPublicDenotable(approximateLocalTypes) ?: this
    }

    /**
     * Approximates [KaType] to a [denotable][KaTypeInformationProvider.isDenotable] subtype.
     *
     * The function returns `null` if the type is already denotable and does not need approximation. Otherwise, for a type `T`, returns a
     * denotable type `S` such that `S <: T`, with all type arguments of `S` also being denotable.
     *
     * @param approximateLocalTypes Whether locally declared types should be approximated to non-local subtypes. Avoiding local type
     *  approximation is sensible when the resulting [KaType] is analyzed in the same local context.
     */
    @KaExperimentalApi
    public fun KaType.approximateToSubPublicDenotable(approximateLocalTypes: Boolean): KaType?

    /**
     * Approximates [KaType] to a [denotable][KaTypeInformationProvider.isDenotable] subtype, or returns the given type itself if it is
     * already denotable.
     *
     * @see approximateToSubPublicDenotable
     */
    @KaExperimentalApi
    public fun KaType.approximateToSubPublicDenotableOrSelf(approximateLocalTypes: Boolean): KaType = withValidityAssertion {
        return approximateToSubPublicDenotable(approximateLocalTypes) ?: this
    }

    /**
     * A [KaType] derived from the given type by taking warning-level nullability annotations into account to determine the type's
     * nullability. If the derived type doesn't differ from the original type, the result is `null`.
     *
     * In general, Java type enhancement allows the Kotlin compiler to infer a more specific nullability for a Java type based on its
     * [nullability annotations](https://kotlinlang.org/docs/java-interop.html#nullability-annotations). Normally, only [strict][org.jetbrains.kotlin.load.java.ReportLevel.STRICT]
     * nullability annotations have an impact on a resolved type's nullability.
     *
     * However, there are also [warning-level][org.jetbrains.kotlin.load.java.ReportLevel.WARN] nullability annotations (by default), such
     * as Android's `RecentlyNullable` and `RecentlyNonNull`. These don't affect a resolved type's nullability. [enhancedType] can be used
     * to obtain a [KaType] which takes warning-level nullability annotations into account for its nullability.
     *
     * See also [NULLABILITY_ANNOTATION_SETTINGS][org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATION_SETTINGS], which is a list of
     * default report levels for different nullability annotations.
     */
    @KaExperimentalApi
    public val KaType.enhancedType: KaType?

    /**
     * @see enhancedType
     */
    @KaExperimentalApi
    public val KaType.enhancedTypeOrSelf: KaType?
        get() = withValidityAssertion { enhancedType ?: this }

    /**
     * Returns the representation of [this] in terms of [KaType].
     *
     * Type parameters are substituted with matching type parameter types, e.g. `List<T>` for the `List` class.
     *
     * @see KaTypeCreator
     */
    public val KaClassifierSymbol.defaultType: KaType

    @Deprecated("Use `defaultType` from `KaClassifierSymbol` directly", level = DeprecationLevel.HIDDEN)
    public val KaNamedClassSymbol.defaultType: KaType get() = defaultType

    /**
     * Computes the common supertype of the given [KaType]s.
     *
     * @throws IllegalArgumentException If the collection of types is empty.
     */
    public val Iterable<KaType>.commonSupertype: KaType

    /**
     * Computes the common supertype of the given [KaType]s.
     *
     * @throws IllegalArgumentException If the array of types is empty.
     */
    public val Array<KaType>.commonSupertype: KaType
        get() = asList().commonSupertype

    /**
     * Resolves the given [KtTypeReference] to its corresponding [KaType].
     *
     * This may raise an exception if the resolution ends up with an unexpected result.
     */
    public val KtTypeReference.type: KaType

    /**
     * Resolves the given [KtDoubleColonExpression] to the [KaType] of its receiver.
     *
     * The result may be `null` if the resolution fails or the resolved callable reference is not a reflection type.
     *
     * #### Example
     *
     * ```kotlin
     * class Foo {
     *     fun bar() { }
     * }
     *
     * val foo = Foo()
     * foo::bar
     * ```
     *
     * Here, `receiverType` for `foo::bar` returns `Foo` (the type of `foo`).
     */
    public val KtDoubleColonExpression.receiverType: KaType?

    /**
     * Creates a new [KaType] based on the given type with the updated nullability specified by [isMarkedNullable].
     */
    public fun KaType.withNullability(isMarkedNullable: Boolean): KaType

    /**
     * Creates a [KaType] based on the given type with the specified [newNullability].
     */
    @Deprecated("Use `withNullability(Boolean)` instead")
    @Suppress("Deprecation")
    public fun KaType.withNullability(newNullability: org.jetbrains.kotlin.analysis.api.types.KaTypeNullability): KaType =
        withNullability(newNullability.isNullable)

    /**
     * Returns the [KaFlexibleType]'s upper bound, or the type itself if it is not flexible.
     */
    public fun KaType.upperBoundIfFlexible(): KaType = withValidityAssertion {
        (this as? KaFlexibleType)?.upperBound ?: this
    }

    /**
     * Returns the [KaFlexibleType]'s lower bound, or the type itself if it is not flexible.
     */
    public fun KaType.lowerBoundIfFlexible(): KaType = withValidityAssertion {
        (this as? KaFlexibleType)?.lowerBound ?: this
    }

    /**
     * Checks whether this [KaType] is compatible with [that] other type. If they are compatible, the types can have a common subtype.
     */
    public fun KaType.hasCommonSubtypeWith(that: KaType): Boolean

    /**
     * Collects all the implicit receiver types available at the given [position]. The resulting list is ordered from the outermost to the
     * innermost receiver type.
     */
    public fun collectImplicitReceiverTypes(position: KtElement): List<KaType>

    /**
     * Returns the direct supertypes of the given [KaType].
     *
     * For flexible types, direct supertypes of both the upper and lower bounds are returned. If that's not desirable, use
     * [directSupertypes] on [KaFlexibleType.upperBound] or [KaFlexibleType.lowerBound].
     *
     * #### Example
     *
     * Given `MutableList<String>`, [directSupertypes] returns `List<String>` and `MutableCollection<String>`
     *
     * @param shouldApproximate Whether to approximate [non-denotable][KaTypeInformationProvider.isDenotable] types. For example, the
     * supertype of `List<out String>` is `Collection<CAPTURED out String>`. With approximation set to `true`, `Collection<out String>` is
     * returned instead.
     */
    public fun KaType.directSupertypes(shouldApproximate: Boolean): Sequence<KaType>

    /**
     * The direct supertypes of the given [KaType].
     *
     * For flexible types, direct supertypes of both the upper and lower bounds are returned. If that's not desirable, use
     * [directSupertypes] on [KaFlexibleType.upperBound] or [KaFlexibleType.lowerBound].
     *
     * [Denotable][KaTypeInformationProvider.isDenotable] types are not approximated.
     *
     * #### Example
     *
     * Given `MutableList<String>`, [directSupertypes] returns `List<String>` and `MutableCollection<String>`
     */
    public val KaType.directSupertypes: Sequence<KaType>
        get() = directSupertypes(shouldApproximate = false)

    /**
     * Returns all supertypes of the given [KaType]. The resulting sequence is ordered by a breadth-first traversal of the class hierarchy,
     * without duplicates.
     *
     * @param shouldApproximate Whether to approximate [non-denotable][KaTypeInformationProvider.isDenotable] types. See [directSupertypes]
     *  for more information.
     */
    public fun KaType.allSupertypes(shouldApproximate: Boolean): Sequence<KaType>

    /**
     * Returns all supertypes of the given [KaType]. The resulting sequence is ordered by a breadth-first traversal of the class hierarchy,
     * without duplicates.
     *
     * [Denotable][KaTypeInformationProvider.isDenotable] types are not approximated.
     */
    public val KaType.allSupertypes: Sequence<KaType>
        get() = allSupertypes(shouldApproximate = false)

    /**
     * This function is provided for a few use-cases where it's hard to go without it.
     *
     * **Please avoid using it**; it will probably be removed in the future.
     *
     * The function is instantly deprecated, so it's not shown in the completion.
     *
     * @receiver A target callable symbol.
     * @return A dispatch receiver type for this symbol if it has any.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Avoid using this function")
    public val KaCallableSymbol.dispatchReceiverType: KaType?

    /**
     * The array type's element type if the given [KaType] is a primitive type array or [Array], and `null` otherwise.
     */
    public val KaType.arrayElementType: KaType?
}

public abstract class KaBuiltinTypes : KaLifetimeOwner {
    /** The [Int] class type. */
    public abstract val int: KaType

    /** The [Long] class type. */
    public abstract val long: KaType

    /** The [Short] class type. */
    public abstract val short: KaType

    /** The [Byte] class type. */
    public abstract val byte: KaType

    /** The [Float] class type. */
    public abstract val float: KaType

    /** The [Double] class type. */
    public abstract val double: KaType

    /** The [Boolean] class type. */
    public abstract val boolean: KaType

    /** The [Char] class type. */
    public abstract val char: KaType

    /** The [String] class type. */
    public abstract val string: KaType

    /** The [Unit] class type. */
    public abstract val unit: KaType

    /** The [Nothing] class type. */
    public abstract val nothing: KaType

    /** The [Any] class type. */
    public abstract val any: KaType

    /** The [Throwable] class type. */
    public abstract val throwable: KaType

    /** The `Any?` type. */
    public abstract val nullableAny: KaType

    /** The `Nothing?` type. */
    public abstract val nullableNothing: KaType

    /** The [Annotation] type. */
    public abstract val annotationType: KaType
}
