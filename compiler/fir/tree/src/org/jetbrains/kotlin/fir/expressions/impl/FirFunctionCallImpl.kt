/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirFunctionCallImpl(
    psi: PsiElement?,
    override var safe: Boolean = false
) : FirFunctionCall(psi), FirModifiableQualifiedAccess<FirNamedReference> {
    override val typeArguments = mutableListOf<FirTypeProjection>()

    override lateinit var calleeReference: FirNamedReference

    override var explicitReceiver: FirExpression? = null

    override var dispatchReceiver: FirExpression = FirNoReceiverExpression

    override var extensionReceiver: FirExpression = FirNoReceiverExpression

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        typeArguments.transformInplace(transformer, data)
        super<FirModifiableQualifiedAccess>.transformChildren(transformer, data)

        return super<FirFunctionCall>.transformChildren(transformer, data)
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return this
    }
}