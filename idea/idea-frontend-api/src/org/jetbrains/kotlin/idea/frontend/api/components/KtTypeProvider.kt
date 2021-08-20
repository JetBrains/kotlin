/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtFlexibleType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtTypeReference

public abstract class KtTypeProvider : KtAnalysisSessionComponent() {
    public abstract val builtinTypes: KtBuiltinTypes

    public abstract fun approximateToSuperPublicDenotableType(type: KtType): KtType?

    public abstract fun buildSelfClassType(symbol: KtNamedClassOrObjectSymbol): KtType

    public abstract fun commonSuperType(types: Collection<KtType>): KtType?

    public abstract fun getKtType(ktTypeReference: KtTypeReference): KtType

    public abstract fun getReceiverTypeForDoubleColonExpression(expression: KtDoubleColonExpression): KtType?

    public abstract fun withNullability(type: KtType, newNullability: KtTypeNullability): KtType
}

public interface KtTypeProviderMixIn : KtAnalysisSessionMixIn {
    public val builtinTypes: KtBuiltinTypes
        get() = analysisSession.typeProvider.builtinTypes

    /**
     * Approximates [KtType] with the a supertype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `T <: S` and `T` and every it type argument is [org.jetbrains.kotlin.idea.frontend.api.types.KtDenotableType]`
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

    public abstract val NULLABLE_ANY: KtType
    public abstract val NULLABLE_NOTHING: KtType
}
