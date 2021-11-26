/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.FirTowerDataMode.*
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import java.util.*

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
}

enum class FirTowerDataMode {
    MEMBER_DECLARATION,
    NESTED_CLASS,
    COMPANION_OBJECT,
    CONSTRUCTOR_HEADER,
    ENUM_ENTRY,
    SPECIAL,
}

class FirTowerDataContextsForClassParts(
    forMemberDeclarations: FirTowerDataContext,
    forNestedClasses: FirTowerDataContext? = null,
    forCompanionObject: FirTowerDataContext? = null,
    forConstructorHeaders: FirTowerDataContext? = null,
    forEnumEntries: FirTowerDataContext? = null,
    val primaryConstructorPureParametersScope: FirLocalScope? = null,
    val primaryConstructorAllParametersScope: FirLocalScope? = null,
) {
    private val modeMap = EnumMap<FirTowerDataMode, FirTowerDataContext>(FirTowerDataMode::class.java)

    init {
        modeMap[MEMBER_DECLARATION] = forMemberDeclarations
        modeMap[NESTED_CLASS] = forNestedClasses
        modeMap[COMPANION_OBJECT] = forCompanionObject
        modeMap[CONSTRUCTOR_HEADER] = forConstructorHeaders
        modeMap[ENUM_ENTRY] = forEnumEntries
    }

    var mode: FirTowerDataMode = MEMBER_DECLARATION

    val forMemberDeclaration: FirTowerDataContext get() = modeMap.getValue(MEMBER_DECLARATION)

    val towerDataContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirTowerDataContext> = mutableMapOf()
    val towerDataContextForCallableReferences: MutableMap<FirCallableReferenceAccess, FirTowerDataContext> = mutableMapOf()

    var currentContext: FirTowerDataContext
        get() = modeMap.getValue(mode)
        set(value) {
            modeMap[mode] = value
        }

    fun setAnonymousFunctionContextIfAny(symbol: FirAnonymousFunctionSymbol) {
        val context = towerDataContextForAnonymousFunctions[symbol]
        if (context != null) {
            mode = SPECIAL
            modeMap[SPECIAL] = context
        }
    }

    fun setCallableReferenceContextIfAny(access: FirCallableReferenceAccess) {
        val context = towerDataContextForCallableReferences[access]
        if (context != null) {
            mode = SPECIAL
            modeMap[SPECIAL] = context
        }
    }
}

// --------------------------------------- Utils ---------------------------------------


fun BodyResolveComponents.createCurrentScopeList(): List<FirScope> =
    towerDataElements.asReversed().mapNotNull { it.scope }
