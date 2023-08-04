/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessExpressionBuilder
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface FirAbstractFunctionCallBuilder : FirQualifiedAccessExpressionBuilder, FirCallBuilder {
    abstract override var coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: MutableList<FirAnnotation>
    abstract override val contextReceiverArguments: MutableList<FirExpression>
    abstract override val typeArguments: MutableList<FirTypeProjection>
    abstract override var explicitReceiver: FirExpression?
    abstract override var dispatchReceiver: FirExpression
    abstract override var extensionReceiver: FirExpression
    abstract override var source: KtSourceElement?
    abstract override var argumentList: FirArgumentList
    abstract var calleeReference: FirNamedReference
    abstract var origin: FirFunctionCallOrigin
    override fun build(): FirFunctionCall
}
