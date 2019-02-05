/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirArraySetCallImpl(
    session: FirSession,
    psi: PsiElement?,
    value: FirExpression,
    operation: FirOperation
) : FirAbstractAssignment(session, psi, value, operation, false), FirArraySetCall {
    override val indexes = mutableListOf<FirExpression>()

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        super<FirArraySetCall>.accept(visitor, data)

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        indexes.transformInplace(transformer, data)

        return super<FirAbstractAssignment>.transformChildren(transformer, data)
    }
}