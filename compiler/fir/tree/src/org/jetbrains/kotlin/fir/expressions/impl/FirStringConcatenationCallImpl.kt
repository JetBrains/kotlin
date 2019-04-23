/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef

class FirStringConcatenationCallImpl(
    session: FirSession,
    psi: PsiElement?
) : FirAbstractCall(session, psi), FirStringConcatenationCall {
    override var typeRef: FirTypeRef = FirImplicitStringTypeRef(session, psi)
}