/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.references.FirNamedReferenceWithCandidateBase
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

open class FirNamedReferenceWithCandidate(
    override val source: KtSourceElement?,
    override val name: Name,
    val candidate: Candidate
) : FirNamedReferenceWithCandidateBase() {
    override val candidateSymbol: FirBasedSymbol<*>
        get() = candidate.symbol

    open val isError: Boolean get() = false

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return this
    }
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
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirPropertyWithExplicitBackingFieldResolvedNamedReference {
        return this
    }
}
