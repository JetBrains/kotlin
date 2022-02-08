/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

fun LLFirEnsureBasedTransformerForReturnTypeCalculator(
    designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculator,
    outerBodyResolveContext: BodyResolveContext?,
): FirDesignatedBodyResolveTransformerForReturnTypeCalculator {

    val designationList = mutableListOf<FirElement>()
    designation.forEachRemaining(designationList::add)
    require(designationList.isNotEmpty()) { "Designation should not be empty" }

    return LLFirEnsureBasedTransformerForReturnTypeCalculatorImpl(
        designationList,
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        returnTypeCalculator,
        outerBodyResolveContext
    )
}


private class LLFirEnsureBasedTransformerForReturnTypeCalculatorImpl(
    designation: List<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculator,
    outerBodyResolveContext: BodyResolveContext?,
) : FirDesignatedBodyResolveTransformerForReturnTypeCalculator(
    designation.iterator(),
    session,
    scopeSession,
    implicitBodyResolveComputationSession,
    returnTypeCalculator,
    outerBodyResolveContext
) {
    private val targetDeclaration = designation.last()

    private fun <T : FirCallableDeclaration> T.ensureReturnType() {
        if (this !== targetDeclaration) return
        if (resolvePhase < FirResolvePhase.TYPES && returnTypeRef is FirResolvedTypeRef) return
        ensureResolved(FirResolvePhase.TYPES)
        if (returnTypeRef is FirImplicitTypeRef) {
            ensureResolved(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        }
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: ResolutionMode): FirSimpleFunction {
        simpleFunction.ensureReturnType()
        return super.transformSimpleFunction(simpleFunction, data)
    }


    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        property.ensureReturnType()
        return super.transformProperty(property, data)
    }
}
