/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractCall(
    session: FirSession,
    psi: PsiElement?
) : FirAbstractExpression(session, psi), FirCall {
    final override val arguments = mutableListOf<FirExpression>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        arguments.transformInplace(transformer, data)
        return super<FirAbstractExpression>.transformChildren(transformer, data)
    }
}