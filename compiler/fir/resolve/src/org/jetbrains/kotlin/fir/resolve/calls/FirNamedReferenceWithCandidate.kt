/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

@OptIn(FirImplementationDetail::class)
open class FirNamedReferenceWithCandidate(
    source: KtSourceElement?,
    name: Name,
    val candidate: Candidate
) : FirSimpleNamedReference(source, name, candidate.symbol) {
    override val candidateSymbol: FirBasedSymbol<*>
        get() = candidate.symbol

    open val isError: Boolean get() = false
}

class FirErrorReferenceWithCandidate(
    source: KtSourceElement?,
    name: Name,
    candidate: Candidate,
    val diagnostic: ConeDiagnostic
) : FirNamedReferenceWithCandidate(source, name, candidate) {
    override val isError: Boolean get() = true
}

class FirPropertyWithExplicitBackingFieldResolvedNamedReference(
    override val source: KtSourceElement?,
    override val name: Name,
    override val resolvedSymbol: FirBasedSymbol<*>,
    val hasVisibleBackingField: Boolean,
) : FirResolvedNamedReference() {
    override val candidateSymbol: FirBasedSymbol<*>? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyWithExplicitBackingFieldResolvedNamedReference {
        return this
    }
}
