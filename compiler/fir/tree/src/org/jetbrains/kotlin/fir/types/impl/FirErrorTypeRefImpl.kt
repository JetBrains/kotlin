/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirErrorTypeRefImpl(
    session: FirSession,
    psi: PsiElement?,
    override val reason: String
) : FirAbstractAnnotatedTypeRef(session, psi, false), FirErrorTypeRef {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirErrorTypeRef>.accept(visitor, data)
    }
}