/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

@OptIn(AdapterForResolveProcessor::class)
class FirContractResolveProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(
    session, scopeSession, FirResolvePhase.CONTRACTS
) {
    override val transformer = FirContractResolveTransformerAdapter(session, scopeSession)
}

@AdapterForResolveProcessor
class FirContractResolveTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Any?>() {
    private val transformer = FirContractResolveTransformer(session, scopeSession)
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            file.transform(transformer, ResolutionMode.ContextIndependent)
        }
    }
}

fun <F : FirClassLikeDeclaration> F.runContractResolveForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: BodyResolveContext,
    targetedClasses: Set<FirClassLikeDeclaration>
): F {
    val newContext = outerBodyResolveContext.createSnapshotForLocalClasses(
        ReturnTypeCalculatorForFullBodyResolve.Contract,
        targetedClasses
    )
    val transformer = FirContractResolveTransformer(session, scopeSession, newContext)

    return this.transformSingle(transformer, ResolutionMode.ContextIndependent)
}

fun <F : FirFunction> F.runContractResolveForFunction(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: BodyResolveContext,
): F {
    val transformer = FirContractResolveTransformer(session, scopeSession, outerBodyResolveContext)

    return this.transformSingle(transformer, ResolutionMode.ContextIndependent)
}
