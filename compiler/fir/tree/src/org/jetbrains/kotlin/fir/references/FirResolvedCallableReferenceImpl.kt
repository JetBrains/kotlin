/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.name.Name

class FirResolvedCallableReferenceImpl(
    session: FirSession,
    psi: PsiElement?,
    override val name: Name,
    override val callableSymbol: ConeCallableSymbol
) : FirAbstractElement(session, psi), FirResolvedCallableReference