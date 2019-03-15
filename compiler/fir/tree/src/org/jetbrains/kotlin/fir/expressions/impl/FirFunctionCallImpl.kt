/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirFunctionCallImpl(
    session: FirSession,
    psi: PsiElement?,
    override var safe: Boolean = false
) : FirAbstractCall(session, psi), FirFunctionCall, FirModifiableQualifiedAccess {
    override val typeArguments = mutableListOf<FirTypeProjection>()

    override lateinit var calleeReference: FirNamedReference

    override var explicitReceiver: FirExpression? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        typeArguments.transformInplace(transformer, data)
        calleeReference = calleeReference.transformSingle(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return super<FirAbstractCall>.transformChildren(transformer, data)
    }
}