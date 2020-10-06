/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessBuilder
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
interface FirAbstractFunctionCallBuilder : FirQualifiedAccessBuilder, FirCallBuilder {
    abstract override var source: FirSourceElement?
    abstract override val annotations: MutableList<FirAnnotationCall>
    abstract override val typeArguments: MutableList<FirTypeProjection>
    abstract override var explicitReceiver: FirExpression?
    abstract override var dispatchReceiver: FirExpression
    abstract override var extensionReceiver: FirExpression
    abstract override var argumentList: FirArgumentList
    abstract var typeRef: FirTypeRef
    abstract var calleeReference: FirNamedReference
    override fun build(): FirFunctionCall
}
