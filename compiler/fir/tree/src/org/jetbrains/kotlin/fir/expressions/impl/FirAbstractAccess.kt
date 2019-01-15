/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractAccess(
    session: FirSession,
    psi: PsiElement?,
    final override var safe: Boolean = false
) : FirAbstractStatement(session, psi), FirModifiableAccess {
    final override lateinit var calleeReference: FirReference

    final override var explicitReceiver: FirExpression? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return super<FirAbstractStatement>.transformChildren(transformer, data)
    }
}