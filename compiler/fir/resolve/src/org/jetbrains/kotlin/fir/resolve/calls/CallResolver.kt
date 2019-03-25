/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processClassifiersByNameWithAction
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.utils.addToStdlib.cast


class CallInfo(
    val variableAccess: Boolean,
    val explicitReceiver: FirExpression?,
    val argumentCount: Int
) {

}

interface CheckerSink {
    fun reportApplicability(new: CandidateApplicability)
}


class CheckerSinkImpl : CheckerSink {
    var current = CandidateApplicability.RESOLVED
    override fun reportApplicability(new: CandidateApplicability) {
        if (new < current) current = new
    }
}


class Candidate(
    val symbol: ConeSymbol,
    val receiverKind: ExplicitReceiverKind,
    val callKind: CallKind
)

sealed class CallKind {
    abstract fun sequence(): List<ResolutionStage>
    object Function : CallKind() {
        override fun sequence(): List<ResolutionStage> {
            return functionCallResolutionSequence()
        }
    }

    object VariableAccess : CallKind() {
        override fun sequence(): List<ResolutionStage> {
            return qualifiedAccessResolutionSequence()
        }
    }
}


enum class TowerDataKind {
    EMPTY,
    TOWER_LEVEL
}

interface TowerScopeLevel {

    sealed class Token<out T : ConeSymbol> {
        object Properties : Token<ConePropertySymbol>()

        object Functions : Token<ConeFunctionSymbol>()
        object Objects : Token<ConeClassifierSymbol>()
    }

    fun <T : ConeSymbol> processElementsByName(
        token: Token<T>,
        name: Name,
        extensionReceiver: ReceiverValueWithPossibleTypes?,
        processor: TowerScopeLevelProcessor<T>
    ): ProcessorAction

    interface TowerScopeLevelProcessor<T : ConeSymbol> {
        fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction
    }

    object Empty : TowerScopeLevel {
        override fun <T : ConeSymbol> processElementsByName(
            token: Token<T>,
            name: Name,
            extensionReceiver: ReceiverValueWithPossibleTypes?,
            processor: TowerScopeLevelProcessor<T>
        ): ProcessorAction = ProcessorAction.NEXT
    }
}

interface ReceiverValue {
    val type: ConeKotlinType
}

interface ReceiverValueWithPossibleTypes : ReceiverValue

class MemberScopeTowerLevel(
    val session: FirSession,
    val dispatchReceiver: ReceiverValueWithPossibleTypes
) : TowerScopeLevel {


    private fun <T : ConeSymbol> processMembers(
        output: TowerScopeLevel.TowerScopeLevelProcessor<T>,
        takeMembers: FirScope.(processor: (T) -> ProcessorAction) -> ProcessorAction
    ): ProcessorAction {
        return dispatchReceiver.type.scope(session)?.takeMembers { output.consumeCandidate(it, dispatchReceiver) } ?: ProcessorAction.NEXT
    }

    override fun <T : ConeSymbol> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        extensionReceiver: ReceiverValueWithPossibleTypes?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        return when (token) {
            TowerScopeLevel.Token.Properties -> processMembers(processor) { this.processPropertiesByName(name, it.cast()) }
            TowerScopeLevel.Token.Functions -> processMembers(processor) { this.processFunctionsByName(name, it.cast()) }
            TowerScopeLevel.Token.Objects -> ProcessorAction.NEXT
        }
    }

}

class ScopeTowerLevel(
    val session: FirSession,
    val scope: FirScope
) : TowerScopeLevel {
    override fun <T : ConeSymbol> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        extensionReceiver: ReceiverValueWithPossibleTypes?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        return when (token) {

            TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(name) { processor.consumeCandidate(it as T, null) }
            TowerScopeLevel.Token.Functions -> scope.processFunctionsByName(name) { processor.consumeCandidate(it as T, null) }
            TowerScopeLevel.Token.Objects -> scope.processClassifiersByNameWithAction(name, FirPosition.OTHER) {
                processor.consumeCandidate(
                    it as T,
                    null
                )
            }
        }
    }

}


abstract class TowerDataConsumer {
    abstract fun consume(
        kind: TowerDataKind,
        implicitReceiverType: ConeKotlinType?,
        towerScopeLevel: TowerScopeLevel,
        resultCollector: CandidateCollector
    ): ProcessorAction
}


fun createVariableConsumer(
    session: FirSession,
    name: Name,
    explicitReceiver: FirExpression?,
    explicitReceiverType: FirTypeRef?
): TowerDataConsumer {
    return createSimpleConsumer(session, name, TowerScopeLevel.Token.Properties, explicitReceiver, explicitReceiverType, CallKind.VariableAccess)
}

fun createFunctionConsumer(
    session: FirSession,
    name: Name,
    explicitReceiver: FirExpression?,
    explicitReceiverType: FirTypeRef?
): TowerDataConsumer {
    return createSimpleConsumer(session, name, TowerScopeLevel.Token.Functions, explicitReceiver, explicitReceiverType, CallKind.Function)
}


fun createSimpleConsumer(
    session: FirSession,
    name: Name,
    token: TowerScopeLevel.Token<*>,
    explicitReceiver: FirExpression?,
    explicitReceiverType: FirTypeRef?,
    callKind: CallKind
): TowerDataConsumer {
    return if (explicitReceiver != null) {
        ExplicitReceiverTowerDataConsumer(session, name, token, object : ReceiverValueWithPossibleTypes {
            override val type: ConeKotlinType
                get() = explicitReceiverType?.coneTypeSafe()
                    ?: ConeKotlinErrorType("No type calculated for: ${explicitReceiver.renderWithType()}") // TODO: assert here
        }, callKind)
    } else {
        NoExplicitReceiverTowerDataConsumer(session, name, token, callKind)
    }
}

class ExplicitReceiverTowerDataConsumer<T : ConeSymbol>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val explicitReceiver: ReceiverValueWithPossibleTypes,
    val callKind: CallKind
) : TowerDataConsumer() {

    var groupId = 0

    override fun consume(
        kind: TowerDataKind,
        implicitReceiverType: ConeKotlinType?,
        towerScopeLevel: TowerScopeLevel,
        resultCollector: CandidateCollector
    ): ProcessorAction {
        groupId++
        return when (kind) {
            TowerDataKind.EMPTY ->
                MemberScopeTowerLevel(session, explicitReceiver).processElementsByName(
                    token,
                    name,
                    null,
                    object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction {
                            resultCollector.consumeCandidate(groupId, Candidate(symbol, ExplicitReceiverKind.DISPATCH_RECEIVER, callKind))
                            return ProcessorAction.NEXT
                        }
                    }
                )
            TowerDataKind.TOWER_LEVEL ->
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    explicitReceiver,
                    object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction {
                            resultCollector.consumeCandidate(groupId, Candidate(symbol, ExplicitReceiverKind.EXTENSION_RECEIVER, callKind))
                            return ProcessorAction.NEXT
                        }
                    }
                )
        }
    }

}

class NoExplicitReceiverTowerDataConsumer<T : ConeSymbol>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val callKind: CallKind
) : TowerDataConsumer() {
    var groupId = 0


    override fun consume(
        kind: TowerDataKind,
        implicitReceiverType: ConeKotlinType?,
        towerScopeLevel: TowerScopeLevel,
        resultCollector: CandidateCollector
    ): ProcessorAction {
        groupId++
        return when (kind) {

            TowerDataKind.TOWER_LEVEL -> {
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    null,
                    object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction {
                            resultCollector.consumeCandidate(groupId, Candidate(symbol, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, callKind))
                            return ProcessorAction.NEXT
                        }
                    }
                )
            }
            else -> ProcessorAction.NEXT
        }
    }

}

class CallResolver(val typeCalculator: ReturnTypeCalculator, val session: FirSession) {

    var callInfo: CallInfo? = null

    var scopes: List<FirScope>? = null

    fun runTowerResolver(towerDataConsumer: TowerDataConsumer): CandidateCollector {
        val collector = CandidateCollector(callInfo!!)

        towerDataConsumer.consume(TowerDataKind.EMPTY, null, TowerScopeLevel.Empty, collector)

        for (scope in scopes!!) {
            towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, null, ScopeTowerLevel(session, scope), collector)
        }

        return collector
    }


}


enum class CandidateApplicability {
    HIDDEN,
    WRONG_RECEIVER,
    PARAMETER_MAPPING_ERROR,
    INAPPLICABLE,
    SYNTHETIC_RESOLVED,
    RESOLVED
}

class CandidateCollector(val callInfo: CallInfo) {

    val groupNumbers = mutableListOf<Int>()
    val candidates = mutableListOf<Candidate>()


    var currentApplicability = CandidateApplicability.HIDDEN


    fun newDataSet() {
        groupNumbers.clear()
        candidates.clear()
        currentApplicability = CandidateApplicability.HIDDEN
    }

    protected fun getApplicability(
        group: Int,
        candidate: Candidate
    ): CandidateApplicability {

        val sink = CheckerSinkImpl()


        candidate.callKind.sequence().forEach {
            it.check(candidate, sink, callInfo)
        }

        return sink.current
    }

    fun consumeCandidate(group: Int, candidate: Candidate) {
        val applicability = getApplicability(group, candidate)

        if (applicability > currentApplicability) {
            groupNumbers.clear()
            candidates.clear()
            currentApplicability = applicability
        }


        if (applicability == currentApplicability) {
            candidates.add(candidate)
            groupNumbers.add(group)
        }
    }


    fun successCandidates(): List<ConeSymbol> {
        if (groupNumbers.isEmpty()) return emptyList()
        val result = mutableListOf<ConeSymbol>()
        var bestGroup = groupNumbers.first()
        for ((index, candidate) in candidates.withIndex()) {
            val group = groupNumbers[index]
            if (bestGroup > group) {
                bestGroup = group
                result.clear()
            }
            if (bestGroup == group) {
                result.add(candidate.symbol)
            }
        }
        return result
    }
}

fun FirCallableDeclaration.dispatchReceiverType(session: FirSession): ConeKotlinType? {
    val id = (this.symbol as ConeCallableSymbol).callableId.classId ?: return null
    val symbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(id) as? FirClassSymbol ?: return null
    return symbol.fir.defaultType()
}