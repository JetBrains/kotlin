/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef

class FirUnitExpression(
    session: FirSession,
    psi: PsiElement?
) : FirAbstractExpression(session, psi) {
    override var typeRef: FirTypeRef = FirImplicitUnitTypeRef(session, psi)
}