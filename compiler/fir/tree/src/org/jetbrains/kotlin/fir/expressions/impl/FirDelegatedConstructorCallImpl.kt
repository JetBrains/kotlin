/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

class FirDelegatedConstructorCallImpl(
    session: FirSession,
    psi: PsiElement?,
    override val constructedType: FirType,
    override val isThis: Boolean
) : FirAbstractCall(session, psi), FirDelegatedConstructorCall {
    override val typeArguments = mutableListOf<FirTypeProjection>()
}