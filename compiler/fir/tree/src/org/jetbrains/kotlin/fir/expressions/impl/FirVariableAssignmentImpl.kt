/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

class FirVariableAssignmentImpl(
    session: FirSession,
    psi: PsiElement?,
    value: FirExpression,
    operation: FirOperation,
    safe: Boolean = false
) : FirAbstractAssignment(session, psi, value, operation, safe), FirVariableAssignment