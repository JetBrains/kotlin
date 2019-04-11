/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirComponentCallImpl(
    session: FirSession,
    psi: PsiElement?,
    override val componentIndex: Int,
    override var explicitReceiver: FirExpression
) : FirAbstractCall(session, psi), FirComponentCall {
    override var calleeReference: FirNamedReference =
        FirSimpleNamedReference(session, psi, Name.identifier("component$componentIndex"))

    override val typeArguments: List<FirTypeProjection>
        get() = emptyList()

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        explicitReceiver = explicitReceiver.transformSingle(transformer, data)
        return super<FirAbstractCall>.transformChildren(transformer, data)
    }
}