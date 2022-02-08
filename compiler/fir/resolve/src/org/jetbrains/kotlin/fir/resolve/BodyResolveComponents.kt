/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.FirTypeRef

data class SessionHolderImpl(override val session: FirSession, override val scopeSession: ScopeSession) : SessionHolder {
    companion object {
        fun createWithEmptyScopeSession(session: FirSession): SessionHolderImpl = SessionHolderImpl(session, ScopeSession())
    }
}

abstract class BodyResolveComponents : SessionHolder {
    abstract val returnTypeCalculator: ReturnTypeCalculator
    abstract val implicitReceiverStack: ImplicitReceiverStack
    abstract val containingDeclarations: List<FirDeclaration>
    abstract val fileImportsScope: List<FirScope>
    abstract val towerDataElements: List<FirTowerDataElement>
    abstract val towerDataContext: FirTowerDataContext
    abstract val localScopes: FirLocalScopes
    abstract val noExpectedType: FirTypeRef
    abstract val symbolProvider: FirSymbolProvider
    abstract val file: FirFile
    abstract val container: FirDeclaration
    abstract val resolutionStageRunner: ResolutionStageRunner
    abstract val samResolver: FirSamResolver
    abstract val callResolver: FirCallResolver
    abstract val callCompleter: FirCallCompleter
    abstract val doubleColonExpressionResolver: FirDoubleColonExpressionResolver
    abstract val syntheticCallGenerator: FirSyntheticCallGenerator
    abstract val dataFlowAnalyzer: FirDataFlowAnalyzer<*>
    abstract val outerClassManager: FirOuterClassManager
    abstract val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
}

// --------------------------------------- Utils ---------------------------------------


fun BodyResolveComponents.createCurrentScopeList(): List<FirScope> =
    towerDataElements.asReversed().mapNotNull { it.scope }
