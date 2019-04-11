/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirTypeOperatorCallImpl(
    session: FirSession,
    psi: PsiElement?,
    operation: FirOperation,
    override var conversionTypeRef: FirTypeRef
) : FirAbstractOperationBasedCall(session, psi, operation), FirTypeOperatorCall {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        conversionTypeRef = conversionTypeRef.transformSingle(transformer, data)
        return super<FirAbstractOperationBasedCall>.transformChildren(transformer, data)
    }
}