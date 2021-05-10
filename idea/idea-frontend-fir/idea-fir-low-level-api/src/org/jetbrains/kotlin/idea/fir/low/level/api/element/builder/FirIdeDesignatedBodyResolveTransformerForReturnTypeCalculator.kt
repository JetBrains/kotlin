/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDesignatedBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyBodiesCalculator

fun FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator(
    designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculator,
    outerBodyResolveContext: BodyResolveContext?,
): FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculatorImpl {

    val designationList = mutableListOf<FirElement>()
    for (element in designation) {
        designationList.add(element)
    }

    return FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculatorImpl(
        designationList,
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        returnTypeCalculator,
        outerBodyResolveContext
    )
}

class FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculatorImpl(
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
    private val declarationDesignation = designation.filterIsInstance<FirDeclaration>()

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        val firDesignation = FirDeclarationDesignation(declarationDesignation, simpleFunction, false)
        FirLazyBodiesCalculator.calculateLazyBodiesForFunction(firDesignation)
        return super.transformSimpleFunction(simpleFunction, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirDeclaration {
        val firDesignation = FirDeclarationDesignation(declarationDesignation, constructor, false)
        FirLazyBodiesCalculator.calculateLazyBodyForSecondaryConstructor(firDesignation)
        return super.transformConstructor(constructor, data)
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        val firDesignation = FirDeclarationDesignation(declarationDesignation, property, false)
        FirLazyBodiesCalculator.calculateLazyBodyForProperty(firDesignation)
        return super.transformProperty(property, data)
    }
}