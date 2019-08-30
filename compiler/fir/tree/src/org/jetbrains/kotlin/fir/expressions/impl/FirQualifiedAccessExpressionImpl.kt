/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirQualifiedAccessExpressionImpl(
    psi: PsiElement?,
    override var safe: Boolean = false
) : FirModifiableQualifiedAccess<FirReference>, FirQualifiedAccessExpression(psi) {
    override lateinit var calleeReference: FirReference

    override var explicitReceiver: FirExpression? = null

    override var dispatchReceiver: FirExpression = FirNoReceiverExpression

    override var extensionReceiver: FirExpression = FirNoReceiverExpression

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        super<FirModifiableQualifiedAccess>.transformChildren(transformer, data)

        return super<FirQualifiedAccessExpression>.transformChildren(transformer, data)
    }
}