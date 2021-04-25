/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability

abstract class KtTypeProvider : KtAnalysisSessionComponent() {
    abstract val builtinTypes: KtBuiltinTypes

    abstract fun approximateToSuperPublicDenotableType(type: KtType): KtType?

    abstract fun buildSelfClassType(symbol: KtNamedClassOrObjectSymbol): KtType

    abstract fun withNullability(type: KtType, newNullability: KtTypeNullability): KtType
}

interface KtTypeProviderMixIn : KtAnalysisSessionMixIn {
    val builtinTypes: KtBuiltinTypes
        get() = analysisSession.typeProvider.builtinTypes

    /**
     * Approximates [KtType] with the a supertype which can be rendered in a source code
     *
     * Return `null` if the type do not need approximation and can be rendered as is
     * Otherwise, for type `T` return type `S` such `T <: S` and `T` and every it type argument is [org.jetbrains.kotlin.idea.frontend.api.types.KtDenotableType]`
     */
    fun KtType.approximateToSuperPublicDenotable(): KtType? =
        analysisSession.typeProvider.approximateToSuperPublicDenotableType(this)

    fun KtNamedClassOrObjectSymbol.buildSelfClassType(): KtType =
        analysisSession.typeProvider.buildSelfClassType(this)

    fun KtType.withNullability(newNullability: KtTypeNullability): KtType =
        analysisSession.typeProvider.withNullability(this, newNullability)
}

@Suppress("PropertyName")
abstract class KtBuiltinTypes : ValidityTokenOwner {
    abstract val INT: KtType
    abstract val LONG: KtType
    abstract val SHORT: KtType
    abstract val BYTE: KtType

    abstract val FLOAT: KtType
    abstract val DOUBLE: KtType

    abstract val BOOLEAN: KtType
    abstract val CHAR: KtType
    abstract val STRING: KtType

    abstract val UNIT: KtType
    abstract val NOTHING: KtType
    abstract val ANY: KtType

    abstract val NULLABLE_ANY: KtType
    abstract val NULLABLE_NOTHING: KtType
}