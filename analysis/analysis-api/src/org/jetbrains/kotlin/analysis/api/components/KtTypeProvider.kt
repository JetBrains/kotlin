/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

public abstract class KaTypeProvider : KaSessionComponent() {
    public abstract val builtinTypes: KaBuiltinTypes

    public abstract fun approximateToSuperPublicDenotableType(type: KaType, approximateLocalTypes: Boolean): KaType?

    public abstract fun approximateToSubPublicDenotableType(type: KaType, approximateLocalTypes: Boolean): KaType?

    public abstract fun getEnhancedType(type: KaType): KaType?

    public abstract fun buildSelfClassType(symbol: KaNamedClassOrObjectSymbol): KaType

    public abstract fun commonSuperType(types: Collection<KaType>): KaType?

    public abstract fun getKtType(ktTypeReference: KtTypeReference): KaType

    public abstract fun getReceiverTypeForDoubleColonExpression(expression: KtDoubleColonExpression): KaType?

    public abstract fun withNullability(type: KaType, newNullability: KaTypeNullability): KaType

    public abstract fun haveCommonSubtype(a: KaType, b: KaType): Boolean

    public abstract fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KaType>

    public abstract fun getDirectSuperTypes(type: KaType, shouldApproximate: Boolean): List<KaType>

    public abstract fun getAllSuperTypes(type: KaType, shouldApproximate: Boolean): List<KaType>

    public abstract fun getDispatchReceiverType(symbol: KaCallableSymbol): KaType?

    public abstract fun getArrayElementType(type: KaType): KaType?
}

public typealias KtTypeProvider = KaTypeProvider

public interface KaTypeProviderMixIn : KaSessionMixIn {
    public val builtinTypes: KaBuiltinTypes
        get() = withValidityAssertion { analysisSession.typeProvider.builtinTypes }

    /**
     * Approximates [KaType] with a supertype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `T <: S` and `T` and every type argument is denotable
     */
    public fun KaType.approximateToSuperPublicDenotable(approximateLocalTypes: Boolean): KaType? =
        withValidityAssertion { analysisSession.typeProvider.approximateToSuperPublicDenotableType(this, approximateLocalTypes) }

    /**
     * Approximates [KaType] with a subtype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `S <: T` and `T` and every type argument is denotable
     */
    public fun KaType.approximateToSubPublicDenotable(approximateLocalTypes: Boolean): KaType? =
        withValidityAssertion { analysisSession.typeProvider.approximateToSubPublicDenotableType(this, approximateLocalTypes) }

    public fun KaType.approximateToSubPublicDenotableOrSelf(approximateLocalTypes: Boolean): KaType =
        withValidityAssertion { approximateToSubPublicDenotable(approximateLocalTypes) ?: this }

    public fun KaType.approximateToSuperPublicDenotableOrSelf(approximateLocalTypes: Boolean): KaType =
        withValidityAssertion { approximateToSuperPublicDenotable(approximateLocalTypes) ?: this }

    /**
     * Returns a warning-level enhanced type for [KaType] if it is present. Otherwise, returns `null`.
     */
    public fun KaType.getEnhancedType(): KaType? = withValidityAssertion { analysisSession.typeProvider.getEnhancedType(this) }

    public fun KaType.getEnhancedTypeOrSelf(): KaType? = withValidityAssertion { getEnhancedType() ?: this }

    public fun KaNamedClassOrObjectSymbol.buildSelfClassType(): KaType =
        withValidityAssertion { analysisSession.typeProvider.buildSelfClassType(this) }

    /**
     * Computes the common super type of the given collection of [KaType].
     *
     * If the collection is empty, it returns `null`.
     */
    public fun commonSuperType(types: Collection<KaType>): KaType? =
        withValidityAssertion { analysisSession.typeProvider.commonSuperType(types) }

    /**
     * Resolve [KtTypeReference] and return corresponding [KaType] if resolved.
     *
     * This may raise an exception if the resolution ends up with an unexpected kind.
     */
    public fun KtTypeReference.getKaType(): KaType =
        withValidityAssertion { analysisSession.typeProvider.getKtType(this) }

    public fun KtTypeReference.getKtType(): KaType = getKaType()

    /**
     * Resolve [KtDoubleColonExpression] and return [KaType] of its receiver.
     *
     * Return `null` if the resolution fails or the resolved callable reference is not a reflection type.
     */
    public fun KtDoubleColonExpression.getReceiverKtType(): KaType? =
        withValidityAssertion { analysisSession.typeProvider.getReceiverTypeForDoubleColonExpression(this) }

    public fun KaType.withNullability(newNullability: KaTypeNullability): KaType =
        withValidityAssertion { analysisSession.typeProvider.withNullability(this, newNullability) }

    public fun KaType.upperBoundIfFlexible(): KaType = withValidityAssertion { (this as? KaFlexibleType)?.upperBound ?: this }
    public fun KaType.lowerBoundIfFlexible(): KaType = withValidityAssertion { (this as? KaFlexibleType)?.lowerBound ?: this }

    /** Check whether this type is compatible with that type. If they are compatible, it means they can have a common subtype. */
    public fun KaType.hasCommonSubTypeWith(that: KaType): Boolean =
        withValidityAssertion { analysisSession.typeProvider.haveCommonSubtype(this, that) }

    /**
     * Gets all the implicit receiver types available at the given position. The type of the outermost receiver appears at the beginning
     * of the returned list.
     */
    public fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KaType> =
        withValidityAssertion { analysisSession.typeProvider.getImplicitReceiverTypesAtPosition(position) }

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
    public fun KaType.getDirectSuperTypes(shouldApproximate: Boolean = false): List<KaType> =
        withValidityAssertion { analysisSession.typeProvider.getDirectSuperTypes(this, shouldApproximate) }

    /**
     * Gets all the super types of the given type. The returned result is ordered by a BFS traversal of the class hierarchy, without any
     * duplicates.
     *
     * @param shouldApproximate see [getDirectSuperTypes]
     */
    public fun KaType.getAllSuperTypes(shouldApproximate: Boolean = false): List<KaType> =
        withValidityAssertion { analysisSession.typeProvider.getAllSuperTypes(this, shouldApproximate) }

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
    public fun KaCallableSymbol.getDispatchReceiverType(): KaType? =
        withValidityAssertion { analysisSession.typeProvider.getDispatchReceiverType(this) }

    /**
     * If provided [KaType] is a primitive type array or [Array], returns the type of the array's elements. Otherwise, returns null.
     */
    public fun KaType.getArrayElementType(): KaType? =
        withValidityAssertion { analysisSession.typeProvider.getArrayElementType(this) }
}

public typealias KtTypeProviderMixIn = KaTypeProviderMixIn

@Suppress("PropertyName")
public abstract class KaBuiltinTypes : KaLifetimeOwner {
    public abstract val int: KaType

    @Deprecated("Use 'int' instead.", replaceWith = ReplaceWith("int"))
    public val INT: KaType
        get() = int

    public abstract val long: KaType

    @Deprecated("Use 'long' instead.", replaceWith = ReplaceWith("long"))
    public val LONG: KaType
        get() = long

    public abstract val short: KaType

    @Deprecated("Use 'short' instead.", replaceWith = ReplaceWith("short"))
    public val SHORT: KaType
        get() = short

    public abstract val byte: KaType

    @Deprecated("Use 'byte' instead.", replaceWith = ReplaceWith("byte"))
    public val BYTE: KaType
        get() = byte

    public abstract val float: KaType

    @Deprecated("Use 'float' instead.", replaceWith = ReplaceWith("float"))
    public val FLOAT: KaType
        get() = float

    public abstract val double: KaType

    @Deprecated("Use 'double' instead.", replaceWith = ReplaceWith("double"))
    public val DOUBLE: KaType
        get() = double

    public abstract val boolean: KaType

    @Deprecated("Use 'boolean' instead.", replaceWith = ReplaceWith("boolean"))
    public val BOOLEAN: KaType
        get() = boolean

    public abstract val char: KaType

    @Deprecated("Use 'char' instead.", replaceWith = ReplaceWith("char"))
    public val CHAR: KaType
        get() = char

    public abstract val string: KaType

    @Deprecated("Use 'string' instead.", replaceWith = ReplaceWith("string"))
    public val STRING: KaType
        get() = string

    public abstract val unit: KaType

    @Deprecated("Use 'unit' instead.", replaceWith = ReplaceWith("unit"))
    public val UNIT: KaType
        get() = unit

    public abstract val nothing: KaType

    @Deprecated("Use 'nothing' instead.", replaceWith = ReplaceWith("nothing"))
    public val NOTHING: KaType
        get() = nothing

    public abstract val any: KaType

    @Deprecated("Use 'any' instead.", replaceWith = ReplaceWith("any"))
    public val ANY: KaType
        get() = any

    public abstract val throwable: KaType

    @Deprecated("Use 'throwable' instead.", replaceWith = ReplaceWith("throwable"))
    public val THROWABLE: KaType
        get() = throwable

    public abstract val nullableAny: KaType

    @Deprecated("Use 'nullableAny' instead.", replaceWith = ReplaceWith("nullableAny"))
    public val NULLABLE_ANY: KaType
        get() = nullableAny

    public abstract val nullableNothing: KaType

    @Deprecated("Use 'nullableNothing' instead.", replaceWith = ReplaceWith("nullableNothing"))
    public val NULLABLE_NOTHING: KaType
        get() = nullableNothing
}

public typealias KtBuiltinTypes = KaBuiltinTypes