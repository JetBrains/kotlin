/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

@AdapterForResolvePhase
class FirContractResolveTransformerAdapter(private val scopeSession: ScopeSession) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val transformer = FirContractResolveTransformer(
            file.session,
            scopeSession
        )
        return file.transform(transformer, ResolutionMode.ContextIndependent)
    }
}

fun <F : FirClass<F>> F.runContractResolveForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: FirAbstractBodyResolveTransformer.BodyResolveContext,
    targetedClasses: Set<FirClass<*>>
): F {
    val newContext = outerBodyResolveContext.createSnapshotForLocalClasses(
        ReturnTypeCalculatorForFullBodyResolve(),
        targetedClasses
    )
    val transformer = FirContractResolveTransformer(session, scopeSession, newContext)

    return this.transformSingle(transformer, ResolutionMode.ContextIndependent)
}