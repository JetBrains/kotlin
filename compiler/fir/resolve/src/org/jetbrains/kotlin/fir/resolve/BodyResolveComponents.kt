/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef

interface SessionHolder {
    val session: FirSession
    val scopeSession: ScopeSession
}

interface BodyResolveComponents : SessionHolder {
    val returnTypeCalculator: ReturnTypeCalculator
    val implicitReceiverStack: MutableImplicitReceiverStack
    val topLevelScopes: List<FirScope>
    val localScopes: FirLocalScopes
    val localContextForAnonymousFunctions: LocalContextForAnonymousFunctions
    val noExpectedType: FirTypeRef
    val symbolProvider: FirSymbolProvider
    val file: FirFile
    val container: FirDeclaration
    val inferenceComponents: InferenceComponents
    val resolutionStageRunner: ResolutionStageRunner
    val samResolver: FirSamResolver
    val callResolver: FirCallResolver
    val callCompleter: FirCallCompleter
    val doubleColonExpressionResolver: FirDoubleColonExpressionResolver
    val syntheticCallGenerator: FirSyntheticCallGenerator
    val dataFlowAnalyzer: FirDataFlowAnalyzer<*>
    val integerLiteralTypeApproximator: IntegerLiteralTypeApproximationTransformer
    val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater

    val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() = phasedFir(FirResolvePhase.DECLARATIONS)

    fun saveContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction)
    fun dropContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction)
}

typealias FirLocalScopes = PersistentList<FirLocalScope>

class FirLocalContext(
    val localScopes: FirLocalScopes,
    val implicitReceiverStack: MutableImplicitReceiverStack
)

typealias LocalContextForAnonymousFunctions = Map<FirAnonymousFunctionSymbol, FirLocalContext>

// --------------------------------------- Utils ---------------------------------------

data class ImplicitReceivers(
    val implicitReceiverValue: ImplicitReceiverValue<*>?,
    val implicitCompanionValues: List<ImplicitReceiverValue<*>>
)

fun SessionHolder.collectImplicitReceivers(
    type: ConeKotlinType?,
    owner: FirDeclaration
): ImplicitReceivers {
    if (type == null) return ImplicitReceivers(null, emptyList())

    val implicitCompanionValues = mutableListOf<ImplicitReceiverValue<*>>()
    val implicitReceiverValue = when (owner) {
        is FirClass<*> -> {
            // Questionable: performance
            (owner as? FirRegularClass)?.companionObject?.let { companion ->
                implicitCompanionValues += ImplicitDispatchReceiverValue(
                    companion.symbol, session, scopeSession, kind = ImplicitDispatchReceiverKind.COMPANION
                )
            }
            lookupSuperTypes(owner, lookupInterfaces = false, deep = true, useSiteSession = session).mapNotNull {
                val superClass = (it as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir as? FirRegularClass
                superClass?.companionObject?.let { companion ->
                    implicitCompanionValues += ImplicitDispatchReceiverValue(
                        companion.symbol, session, scopeSession, kind = ImplicitDispatchReceiverKind.COMPANION_FROM_SUPERTYPE
                    )
                }
            }
            // ---
            ImplicitDispatchReceiverValue(owner.symbol, type, session, scopeSession)
        }
        is FirFunction<*> -> {
            ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
        }
        is FirVariable<*> -> {
            ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
        }
        else -> {
            throw IllegalArgumentException("Incorrect label & receiver owner: ${owner.javaClass}")
        }
    }
    return ImplicitReceivers(implicitReceiverValue, implicitCompanionValues.asReversed())
}
