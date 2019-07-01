/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractQualifiedAccess(
    session: FirSession,
    psi: PsiElement?,
    final override var safe: Boolean = false
) : FirAnnotatedStatement(session, psi), FirModifiableQualifiedAccess<FirReference> {
    final override lateinit var calleeReference: FirReference

    final override var explicitReceiver: FirExpression? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)
        return super<FirAnnotatedStatement>.transformChildren(transformer, data)
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