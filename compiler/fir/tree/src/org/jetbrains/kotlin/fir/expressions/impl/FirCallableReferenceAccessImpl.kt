/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirCallableReferenceAccessImpl(
    psi: PsiElement?
) : FirCallableReferenceAccess(psi), FirModifiableQualifiedAccess<FirReference> {
    override lateinit var calleeReference: FirReference

    override var explicitReceiver: FirExpression? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        explicitReceiver = explicitReceiver?.transformSingle(transformer, data)

        return super<FirCallableReferenceAccess>.transformChildren(transformer, data)
    }
}