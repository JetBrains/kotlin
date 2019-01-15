/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.ir.expressions.IrConstKind

class FirConstExpressionImpl<T>(
    session: FirSession,
    psi: PsiElement?,
    override val kind: IrConstKind<T>,
    override val value: T
) : FirAbstractExpression(session, psi), FirConstExpression<T>

fun <T> FirConstExpressionImpl(session: FirSession, psi: PsiElement?, kind: IrConstKind<T>, value: T?, errorReason: String) =
    value?.let { FirConstExpressionImpl(session, psi, kind, it) } ?: FirErrorExpressionImpl(session, psi, errorReason)