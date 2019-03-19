/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef

fun FirFunctionCall.copy(
    annotations: List<FirAnnotationCall> = this.annotations,
    arguments: List<FirExpression> = this.arguments,
    calleeReference: FirNamedReference = this.calleeReference,
    explicitReceiver: FirExpression? = this.explicitReceiver,
    psi: PsiElement? = this.psi,
    safe: Boolean = this.safe,
    session: FirSession = this.session,
    typeArguments: List<FirTypeProjection> = this.typeArguments,
    resultType: FirTypeRef = this.typeRef
): FirFunctionCall {
    return FirFunctionCallImpl(
        session, psi, safe
    ).apply {
        this.annotations.addAll(annotations)
        this.arguments.addAll(arguments)
        this.calleeReference = calleeReference
        this.explicitReceiver = explicitReceiver
        this.typeArguments.addAll(typeArguments)
        this.typeRef = resultType
    }
}

