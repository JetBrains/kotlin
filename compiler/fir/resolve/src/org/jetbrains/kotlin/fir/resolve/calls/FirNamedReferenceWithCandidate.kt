/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.name.Name

class FirNamedReferenceWithCandidate(
    source: FirSourceElement?,
    name: Name,
    val candidate: Candidate
) : FirSimpleNamedReference(source, name, candidate.symbol) {
    override val candidateSymbol: AbstractFirBasedSymbol<*>
        get() = candidate.symbol
}