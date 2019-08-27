/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirThisReference
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirThisReceiverExpressionImpl(
    psi: PsiElement?,
    override var calleeReference: FirThisReference
) : FirModifiableQualifiedAccess<FirThisReference>, FirQualifiedAccessExpression(psi) {
    override var explicitReceiver: FirExpression?
        get() = null
        set(_) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        dispatchReceiver.transformSingle(transformer, data)
        extensionReceiver.transformSingle(transformer, data)

        return super<FirQualifiedAccessExpression>.transformChildren(transformer, data)
    }
}