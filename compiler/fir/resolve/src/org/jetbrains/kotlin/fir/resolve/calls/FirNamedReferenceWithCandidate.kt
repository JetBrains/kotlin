/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.Name

class FirNamedReferenceWithCandidate(session: FirSession, psi: PsiElement?, name: Name, val candidate: Candidate) :
    FirSimpleNamedReference(session, psi, name) {
    override val candidateSymbol: ConeSymbol
        get() = candidate.symbol
}