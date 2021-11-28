/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.analysis.api.types.KtFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

public abstract class KtTypeProvider : KtAnalysisSessionComponent() {
    public abstract val builtinTypes: KtBuiltinTypes

    public abstract fun approximateToSuperPublicDenotableType(type: KtType): KtType?

    public abstract fun buildSelfClassType(symbol: KtNamedClassOrObjectSymbol): KtType

    public abstract fun commonSuperType(types: Collection<KtType>): KtType?

    public abstract fun getKtType(ktTypeReference: KtTypeReference): KtType

    public abstract fun getReceiverTypeForDoubleColonExpression(expression: KtDoubleColonExpression): KtType?

    public abstract fun withNullability(type: KtType, newNullability: KtTypeNullability): KtType

    public abstract fun haveCommonSubtype(a: KtType, b: KtType): Boolean

    public abstract fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KtType>

    public abstract fun getDirectSuperTypes(type: KtType, shouldApproximate: Boolean): List<KtType>

    public abstract fun getAllSuperTypes(type: KtType, shouldApproximate: Boolean): List<KtType>

    public abstract fun getDispatchReceiverType(symbol: KtPossibleMemberSymbol): KtType?
}

public interface KtTypeProviderMixIn : KtAnalysisSessionMixIn {
    public val builtinTypes: KtBuiltinTypes
        get() = analysisSession.typeProvider.builtinTypes

    /**
     * Approximates [KtType] with the a supertype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `T <: S` and `T` and every it type argument is [org.jetbrains.kotlin.analysis.api.types.KtDenotableType]`
     */
    public fun KtType.approximateToSuperPublicDenotable(): KtType? =
        analysisSession.typeProvider.approximateToSuperPublicDenotableType(this)

    public fun KtType.approximateToSuperPublicDenotableOrSelf(): KtType = approximateToSuperPublicDenotable() ?: this

    public fun KtNamedClassOrObjectSymbol.buildSelfClassType(): KtType =
        analysisSession.typeProvider.buildSelfClassType(this)

    /**
     * Computes the common super type of the given collection of [KtType].
     *
     * If the collection is empty, it returns `null`.
     */
    public fun commonSuperType(types: Collection<KtType>): KtType? =
        analysisSession.typeProvider.commonSuperType(types)

    /**
     * Resolve [KtTypeReference] and return corresponding [KtType] if resolved.
     *
     * This may raise an exception if the resolution ends up with an unexpected kind.
     */
    public fun KtTypeReference.getKtType(): KtType =
        analysisSession.typeProvider.getKtType(this)

    /**
     * Resolve [KtDoubleColonExpression] and return [KtType] of its receiver.
     *
     * Return `null` if the resolution fails or the resolved callable reference is not a reflection type.
     */
    public fun KtDoubleColonExpression.getReceiverKtType(): KtType? =
        analysisSession.typeProvider.getReceiverTypeForDoubleColonExpression(this)

    public fun KtType.withNullability(newNullability: KtTypeNullability): KtType =
        analysisSession.typeProvider.withNullability(this, newNullability)

    public fun KtType.upperBoundIfFlexible(): KtType = (this as? KtFlexibleType)?.upperBound ?: this
    public fun KtType.lowerBoundIfFlexible(): KtType = (this as? KtFlexibleType)?.lowerBound ?: this

    /** Check whether this type is compatible with that type. If they are compatible, it means they can have a common subtype. */
    public fun KtType.hasCommonSubTypeWith(that: KtType): Boolean = analysisSession.typeProvider.haveCommonSubtype(this, that)

    /**
     * Gets all the implicit receiver types available at the given position. The type of the outermost receiver appears at the beginning
     * of the returned list.
     */
    public fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KtType> =
        analysisSession.typeProvider.getImplicitReceiverTypesAtPosition(position)

    /**
     * Gets the direct super types of the given type. For example, given `MutableList<String>`, this returns `List<String>` and
     * `MutableCollection<String>`.
     *
     * Note that for flexible types, both direct super types of the upper and lower bounds are returned. If that's not desirable, please
     * first call [KtFlexibleType.upperBound] or [KtFlexibleType.lowerBound] and then call this method.
     *
     * @param shouldApproximate whether to approximate non-denotable types. For example, super type of `List<out String>` is
     * `Collection<CAPTURED out String>`. With approximation set to true, `Collection<out String>` is returned instead.
     */
    public fun KtType.getDirectSuperTypes(shouldApproximate: Boolean = false): List<KtType> =
        analysisSession.typeProvider.getDirectSuperTypes(this, shouldApproximate)

    /**
     * Gets all the super types of the given type. The returned result is ordered by a BFS traversal of the class hierarchy, without any
     * duplicates.
     *
     * @param shouldApproximate see [getDirectSuperTypes]
     */
    public fun KtType.getAllSuperTypes(shouldApproximate: Boolean = false): List<KtType> =
        analysisSession.typeProvider.getAllSuperTypes(this, shouldApproximate)

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
    public fun KtPossibleMemberSymbol.getDispatchReceiverType(): KtType? =
        analysisSession.typeProvider.getDispatchReceiverType(this)
}

@Suppress("PropertyName")
public abstract class KtBuiltinTypes : ValidityTokenOwner {
    public abstract val INT: KtType
    public abstract val LONG: KtType
    public abstract val SHORT: KtType
    public abstract val BYTE: KtType

    public abstract val FLOAT: KtType
    public abstract val DOUBLE: KtType

    public abstract val BOOLEAN: KtType
    public abstract val CHAR: KtType
    public abstract val STRING: KtType

    public abstract val UNIT: KtType
    public abstract val NOTHING: KtType
    public abstract val ANY: KtType

    public abstract val THROWABLE: KtType

    public abstract val NULLABLE_ANY: KtType
    public abstract val NULLABLE_NOTHING: KtType
}
