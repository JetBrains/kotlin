/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAssignment
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractAssignment(
    session: FirSession,
    psi: PsiElement?,
    final override var value: FirExpression,
    final override val operation: FirOperation,
    safe: Boolean = false
) : FirAbstractAccess(session, psi, safe), FirPropertyAssignment {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        value = value.transformSingle(transformer, data)
        return super<FirAbstractAccess>.transformChildren(transformer, data)
    }
}