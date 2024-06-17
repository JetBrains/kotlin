/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
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

    public val KaNamedClassSymbol.defaultType: KaType

    @Deprecated("Use 'defaultType' instead.", replaceWith = ReplaceWith("defaultType"))
    public fun KaNamedClassSymbol.buildSelfClassType(): KaType = defaultType

    /**
     * Computes the common super type of the given collection of [KaType].
     *
     * If the collection is empty, it returns `null`.
     */
    public val Iterable<KaType>.commonSupertype: KaType

    public val Array<KaType>.commonSupertype: KaType
        get() = asList().commonSupertype

    @Deprecated("Use 'commonSupertype' instead.", replaceWith = ReplaceWith("types.commonSupertype"))
    public fun commonSuperType(types: Collection<KaType>): KaType? {
        return if (types.isEmpty()) null else types.commonSupertype
    }

    /**
     * Resolve [KtTypeReference] and return corresponding [KaType] if resolved.
     *
     * This may raise an exception if the resolution ends up with an unexpected kind.
     */
    public val KtTypeReference.type: KaType

    @Deprecated("Use 'type' instead.", replaceWith = ReplaceWith("type"))
    public fun KtTypeReference.getKaType(): KaType = type

    @Deprecated("Use 'type' instead.", replaceWith = ReplaceWith("type"))
    public fun KtTypeReference.getKtType(): KaType = type

    /**
     * Resolve [KtDoubleColonExpression] and return [KaType] of its receiver.
     *
     * Return `null` if the resolution fails or the resolved callable reference is not a reflection type.
     */
    public val KtDoubleColonExpression.receiverType: KaType?

    @Deprecated("Use 'receiverType' instead.", replaceWith = ReplaceWith("receiverType"))
    public fun KtDoubleColonExpression.getReceiverKtType(): KaType? = receiverType

    public fun KaType.withNullability(newNullability: KaTypeNullability): KaType

    public fun KaType.upperBoundIfFlexible(): KaType = withValidityAssertion {
        (this as? KaFlexibleType)?.upperBound ?: this
    }

    public fun KaType.lowerBoundIfFlexible(): KaType = withValidityAssertion {
        (this as? KaFlexibleType)?.lowerBound ?: this
    }

    /** Check whether this type is compatible with that type. If they are compatible, it means they can have a common subtype. */
    public fun KaType.hasCommonSubtypeWith(that: KaType): Boolean

    @Deprecated("Use 'hasCommonSubtypeWith() instead.", replaceWith = ReplaceWith("hasCommonSubtypeWith(that)"))
    public fun KaType.hasCommonSubTypeWith(that: KaType): Boolean = hasCommonSubtypeWith(that)

    /**
     * Gets all the implicit receiver types available at the given position. The type of the outermost receiver appears at the beginning
     * of the returned list.
     */
    public fun collectImplicitReceiverTypes(position: KtElement): List<KaType>

    @Deprecated(
        "Use 'collectImplicitReceiverTypes()' instead.",
        replaceWith = ReplaceWith("collectImplicitReceiverTypes(position)")
    )
    public fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KaType> = collectImplicitReceiverTypes(position)

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

    @Deprecated("Use 'directSuperTypes()' instead.", replaceWith = ReplaceWith("directSuperTypes(shouldApproximate)"))
    public fun KaType.getDirectSuperTypes(shouldApproximate: Boolean = false): List<KaType> = directSupertypes(shouldApproximate).toList()

    /**
     * Gets all the super types of the given type. The returned result is ordered by a BFS traversal of the class hierarchy, without any
     * duplicates.
     *
     * @param shouldApproximate see [getDirectSuperTypes]
     */
    public fun KaType.allSupertypes(shouldApproximate: Boolean): Sequence<KaType>

    /**
     * Gets all the super types of the given type. The returned result is ordered by a BFS traversal of the class hierarchy, without any
     * duplicates.
     */
    public val KaType.allSupertypes: Sequence<KaType>
        get() = allSupertypes(shouldApproximate = false)

    @Deprecated("Use 'allSupertypes()' instead.", replaceWith = ReplaceWith("allSupertypes(shouldApproximate)"))
    public fun KaType.getAllSuperTypes(shouldApproximate: Boolean = false): List<KaType> = allSupertypes(shouldApproximate).toList()

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

@Suppress("PropertyName")
public abstract class KaBuiltinTypes : KaLifetimeOwner {
    /** The [Int] class type. */
    public abstract val int: KaType

    @Deprecated("Use 'int' instead.", replaceWith = ReplaceWith("int"))
    public val INT: KaType
        get() = int

    /** The [Long] class type. */
    public abstract val long: KaType

    @Deprecated("Use 'long' instead.", replaceWith = ReplaceWith("long"))
    public val LONG: KaType
        get() = long

    /** The [Short] class type. */
    public abstract val short: KaType

    @Deprecated("Use 'short' instead.", replaceWith = ReplaceWith("short"))
    public val SHORT: KaType
        get() = short

    /** The [Byte] class type. */
    public abstract val byte: KaType

    @Deprecated("Use 'byte' instead.", replaceWith = ReplaceWith("byte"))
    public val BYTE: KaType
        get() = byte

    /** The [Float] class type. */
    public abstract val float: KaType

    @Deprecated("Use 'float' instead.", replaceWith = ReplaceWith("float"))
    public val FLOAT: KaType
        get() = float

    /** The [Double] class type. */
    public abstract val double: KaType

    @Deprecated("Use 'double' instead.", replaceWith = ReplaceWith("double"))
    public val DOUBLE: KaType
        get() = double

    /** The [Boolean] class type. */
    public abstract val boolean: KaType

    @Deprecated("Use 'boolean' instead.", replaceWith = ReplaceWith("boolean"))
    public val BOOLEAN: KaType
        get() = boolean

    /** The [Char] class type. */
    public abstract val char: KaType

    @Deprecated("Use 'char' instead.", replaceWith = ReplaceWith("char"))
    public val CHAR: KaType
        get() = char

    /** The [String] class type. */
    public abstract val string: KaType

    @Deprecated("Use 'string' instead.", replaceWith = ReplaceWith("string"))
    public val STRING: KaType
        get() = string

    /** The [Unit] class type. */
    public abstract val unit: KaType

    @Deprecated("Use 'unit' instead.", replaceWith = ReplaceWith("unit"))
    public val UNIT: KaType
        get() = unit

    /** The [Nothing] class type. */
    public abstract val nothing: KaType

    @Deprecated("Use 'nothing' instead.", replaceWith = ReplaceWith("nothing"))
    public val NOTHING: KaType
        get() = nothing

    /** The [Any] class type. */
    public abstract val any: KaType

    @Deprecated("Use 'any' instead.", replaceWith = ReplaceWith("any"))
    public val ANY: KaType
        get() = any

    /** The [Throwable] class type. */
    public abstract val throwable: KaType

    @Deprecated("Use 'throwable' instead.", replaceWith = ReplaceWith("throwable"))
    public val THROWABLE: KaType
        get() = throwable

    /** The `Any?` type. */
    public abstract val nullableAny: KaType

    @Deprecated("Use 'nullableAny' instead.", replaceWith = ReplaceWith("nullableAny"))
    public val NULLABLE_ANY: KaType
        get() = nullableAny

    /** The `Nothing?` type. */
    public abstract val nullableNothing: KaType

    @Deprecated("Use 'nullableNothing' instead.", replaceWith = ReplaceWith("nullableNothing"))
    public val NULLABLE_NOTHING: KaType
        get() = nullableNothing
}

@Deprecated("Use 'KaBuiltinTypes' instead.", replaceWith = ReplaceWith("KaBuiltinTypes"))
public typealias KtBuiltinTypes = KaBuiltinTypes