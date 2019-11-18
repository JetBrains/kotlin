/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirAnonymousFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirTryExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenExpressionImpl
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl

fun FirFunctionCall.copy(
    annotations: List<FirAnnotationCall> = this.annotations,
    arguments: List<FirExpression> = this.arguments,
    calleeReference: FirNamedReference = this.calleeReference,
    explicitReceiver: FirExpression? = this.explicitReceiver,
    dispatchReceiver: FirExpression = this.dispatchReceiver,
    extensionReceiver: FirExpression = this.extensionReceiver,
    source: FirSourceElement? = this.source,
    safe: Boolean = this.safe,
    typeArguments: List<FirTypeProjection> = this.typeArguments,
    resultType: FirTypeRef = this.typeRef
): FirFunctionCall {
    return FirFunctionCallImpl(source).apply {
        this.safe = safe
        this.annotations.addAll(annotations)
        this.arguments.addAll(arguments)
        this.calleeReference = calleeReference
        this.explicitReceiver = explicitReceiver
        this.dispatchReceiver = dispatchReceiver
        this.extensionReceiver = extensionReceiver
        this.typeArguments.addAll(typeArguments)
        this.typeRef = resultType
    }
}

fun FirAnonymousFunction.copy(
    receiverTypeRef: FirTypeRef? = this.receiverTypeRef,
    source: FirSourceElement? = this.source,
    session: FirSession = this.session,
    returnTypeRef: FirTypeRef = this.returnTypeRef,
    valueParameters: List<FirValueParameter> = this.valueParameters,
    body: FirBlock? = this.body,
    annotations: List<FirAnnotationCall> = this.annotations,
    typeRef: FirTypeRef = this.typeRef,
    label: FirLabel? = this.label,
    controlFlowGraphReference: FirControlFlowGraphReference = this.controlFlowGraphReference,
    invocationKind: InvocationKind? = this.invocationKind
): FirAnonymousFunction {
    return FirAnonymousFunctionImpl(source, session, returnTypeRef, receiverTypeRef, symbol, isLambda).apply {
        this.valueParameters.addAll(valueParameters)
        this.body = body
        this.annotations.addAll(annotations)
        this.typeRef = typeRef
        this.label = label
        this.controlFlowGraphReference = controlFlowGraphReference
        this.invocationKind = invocationKind
    }
}


fun FirTypeRef.resolvedTypeFromPrototype(
    type: ConeKotlinType
): FirResolvedTypeRef {
    return FirResolvedTypeRefImpl(source, type).apply {
        annotations += this@resolvedTypeFromPrototype.annotations
    }
}

fun FirTypeParameter.copy(
    bounds: List<FirTypeRef> = this.bounds,
    annotations: List<FirAnnotationCall> = this.annotations
): FirTypeParameterImpl {
    return FirTypeParameterImpl(
        source, session, name, symbol, variance, isReified
    ).apply {
        this.bounds += bounds
        this.annotations += annotations
    }
}

fun FirWhenExpression.copy(
    resultType: FirTypeRef = this.typeRef,
    calleeReference: FirReference = this.calleeReference
): FirWhenExpressionImpl = FirWhenExpressionImpl(source, subject, subjectVariable).apply {
    this.calleeReference = calleeReference
    this@apply.branches.addAll(this@copy.branches)
    this.typeRef = resultType
    this.calleeReference = calleeReference
}

fun FirTryExpression.copy(
    resultType: FirTypeRef = this.typeRef,
    calleeReference: FirReference = this.calleeReference
): FirTryExpressionImpl = FirTryExpressionImpl(source, tryBlock, finallyBlock).apply {
    this.calleeReference = calleeReference
    this@apply.catches.addAll(this@copy.catches)
    this.typeRef = resultType
}