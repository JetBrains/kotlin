/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.references.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirDelegatedConstructorCallImpl(
    session: FirSession,
    psi: PsiElement?,
    override var constructedTypeRef: FirTypeRef,
    override val isThis: Boolean
) : FirAbstractCall(session, psi), FirDelegatedConstructorCall {
    override var calleeReference: FirReference =
        if (isThis) FirExplicitThisReference(session, psi, null) else FirExplicitSuperReference(session, psi, constructedTypeRef)

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        constructedTypeRef = constructedTypeRef.transformSingle(transformer, data)
        calleeReference = calleeReference.transformSingle(transformer, data)

        return super<FirAbstractCall>.transformChildren(transformer, data)
    }
}