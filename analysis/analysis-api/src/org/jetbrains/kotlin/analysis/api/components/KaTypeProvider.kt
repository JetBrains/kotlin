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
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

public interface KaTypeProvider {
    public val builtinTypes: KaBuiltinTypes

    /**
     * Approximates [KaType] with a supertype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `T <: S` and `T` and every type argument is denotable
     */
    @KaExperimentalApi
    public fun KaType.approximateToSuperPublicDenotable(approximateLocalTypes: Boolean): KaType?

    /**
     * Approximates [KaType] with a subtype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `S <: T` and `T` and every type argument is denotable
     */
    @KaExperimentalApi
    public fun KaType.approximateToSubPublicDenotable(approximateLocalTypes: Boolean): KaType?

    @KaExperimentalApi
    public fun KaType.approximateToSubPublicDenotableOrSelf(approximateLocalTypes: Boolean): KaType = withValidityAssertion {
        return approximateToSubPublicDenotable(approximateLocalTypes) ?: this
    }

    @KaExperimentalApi
    public fun KaType.approximateToSuperPublicDenotableOrSelf(approximateLocalTypes: Boolean): KaType = withValidityAssertion {
        return approximateToSuperPublicDenotable(approximateLocalTypes) ?: this
    }

    /**
     * Returns a warning-level enhanced type for [KaType] if it is present. Otherwise, returns `null`.
     */
    @KaExperimentalApi
    public val KaType.enhancedType: KaType?

    @KaExperimentalApi
    public val KaType.enhancedTypeOrSelf: KaType?
        get() = withValidityAssertion { enhancedType ?: this }

    /**
     * Returns the representation of [this] in terms of [KaType].
     *
     * @see KaTypeCreator
     */
    public val KaClassifierSymbol.defaultType: KaType

    @Deprecated("Use `defaultType` from `KaClassifierSymbol` directly", level = DeprecationLevel.HIDDEN)
    public val KaNamedClassSymbol.defaultType: KaType get() = defaultType

    /**
     * Computes the common super type of the given collection of [KaType].
     *
     * If the collection is empty, it returns `null`.
     */
    public val Iterable<KaType>.commonSupertype: KaType

    public val Array<KaType>.commonSupertype: KaType
        get() = asList().commonSupertype

    /**
     * Resolve [KtTypeReference] and return corresponding [KaType] if resolved.
     *
     * This may raise an exception if the resolution ends up with an unexpected kind.
     */
    public val KtTypeReference.type: KaType

    /**
     * Resolve [KtDoubleColonExpression] and return [KaType] of its receiver.
     *
     * Return `null` if the resolution fails or the resolved callable reference is not a reflection type.
     */
    public val KtDoubleColonExpression.receiverType: KaType?

    public fun KaType.withNullability(newNullability: KaTypeNullability): KaType

    public fun KaType.upperBoundIfFlexible(): KaType = withValidityAssertion {
        (this as? KaFlexibleType)?.upperBound ?: this
    }

    public fun KaType.lowerBoundIfFlexible(): KaType = withValidityAssertion {
        (this as? KaFlexibleType)?.lowerBound ?: this
    }

    /** Check whether this type is compatible with that type. If they are compatible, it means they can have a common subtype. */
    public fun KaType.hasCommonSubtypeWith(that: KaType): Boolean

    /**
     * Gets all the implicit receiver types available at the given position. The type of the outermost receiver appears at the beginning
     * of the returned list.
     */
    public fun collectImplicitReceiverTypes(position: KtElement): List<KaType>

    /**
     * Gets the direct super types of the given type. For example, given `MutableList<String>`, this returns `List<String>` and
     * `MutableCollection<String>`.
     *
     * Note that for flexible types, both direct super types of the upper and lower bounds are returned. If that's not desirable, please
     * first call [KaFlexibleType.upperBound] or [KaFlexibleType.lowerBound] and then call this method.
     *
     * @param shouldApproximate whether to approximate non-denotable types. For example, super type of `List<out String>` is
     * `Collection<CAPTURED out String>`. With approximation set to true, `Collection<out String>` is returned instead.
     */
    public fun KaType.directSupertypes(shouldApproximate: Boolean): Sequence<KaType>

    /**
     * Gets the direct super types of the given type. For example, given `MutableList<String>`, this returns `List<String>` and
     * `MutableCollection<String>`.
     *
     * Note that for flexible types, both direct super types of the upper and lower bounds are returned. If that's not desirable, please
     * first call [KaFlexibleType.upperBound] or [KaFlexibleType.lowerBound] and then call this method.
     */
    public val KaType.directSupertypes: Sequence<KaType>
        get() = directSupertypes(shouldApproximate = false)

    /**
     * Gets all the super types of the given type. The returned result is ordered by a BFS traversal of the class hierarchy, without any
     * duplicates.
     *
     * @param shouldApproximate see [directSupertypes]
     */
    public fun KaType.allSupertypes(shouldApproximate: Boolean): Sequence<KaType>

    /**
     * Gets all the super types of the given type. The returned result is ordered by a BFS traversal of the class hierarchy, without any
     * duplicates.
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
     * If provided [KaType] is a primitive type array or [Array], returns the type of the array's elements. Otherwise, returns null.
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
