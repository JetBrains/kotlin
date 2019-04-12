/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processClassifiersByNameWithAction
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.utils.addToStdlib.cast




class CallInfo(
    val callKind: CallKind,

    val explicitReceiver: FirExpression?,

    val arguments: List<FirExpression>,
    val typeArguments: List<FirTypeProjection>,

    val typeProvider: (FirExpression) -> FirTypeRef?
) {
    val argumentCount get() = arguments.size
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
    private val inferenceComponents: InferenceComponents,
    private val baseSystem: ConstraintStorage
) {
    val system by lazy {
        val system = inferenceComponents.createConstraintSystem()
        system.addOtherSystem(baseSystem)
        system
    }
    lateinit var substitutor: ConeSubstitutor

    var argumentMapping: Map<FirExpression, FirValueParameter>? = null
}

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
        processScopeMembers: FirScope.(processor: (T) -> ProcessorAction) -> ProcessorAction
    ): ProcessorAction {
        val scope = dispatchReceiver.type.scope(session) ?: return ProcessorAction.NEXT
        if (scope.processScopeMembers { output.consumeCandidate(it, dispatchReceiver) }.stop()) return ProcessorAction.STOP
        val withSynthetic = FirSyntheticPropertiesScope(session, scope, ReturnTypeCalculatorWithJump(session))
        return withSynthetic.processScopeMembers { output.consumeCandidate(it, dispatchReceiver) }
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

private fun ConeCallableSymbol.hasExtensionReceiver(): Boolean = (this as? FirCallableSymbol)?.fir?.receiverTypeRef != null

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

            TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(name) { candidate ->
                val candidateHasExtensionReceiver = candidate.hasExtensionReceiver()
                if (candidateHasExtensionReceiver == (extensionReceiver != null)) {
                    processor.consumeCandidate(candidate as T, boundDispatchReceiver = null)
                } else {
                    ProcessorAction.NEXT
                }
            }
            TowerScopeLevel.Token.Functions -> scope.processFunctionsByName(name) { candidate ->
                val candidateHasExtensionReceiver = candidate.hasExtensionReceiver()
                if (candidateHasExtensionReceiver == (extensionReceiver != null)) {
                    processor.consumeCandidate(candidate as T, boundDispatchReceiver = null)
                } else {
                    ProcessorAction.NEXT
                }
            }
            TowerScopeLevel.Token.Objects -> scope.processClassifiersByNameWithAction(name, FirPosition.OTHER) {
                processor.consumeCandidate(
                    it as T,
                    boundDispatchReceiver = null
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
        resultCollector: CandidateCollector,
        group: Int
    ): ProcessorAction

    private var stopGroup = Int.MAX_VALUE
    fun checkSkip(group: Int, resultCollector: CandidateCollector): Boolean {
        if (resultCollector.isSuccess() && stopGroup == Int.MAX_VALUE) {
            stopGroup = group
        }
        return group > stopGroup
    }
}


fun createVariableAndObjectConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents
): TowerDataConsumer {
    return PrioritizedTowerDataConsumer(
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Properties,
            callInfo,
            inferenceComponents
        ),
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Objects,
            callInfo,
            inferenceComponents
        )
    )

}

fun createFunctionConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents
): TowerDataConsumer {
    return createSimpleConsumer(
        session,
        name,
        TowerScopeLevel.Token.Functions,
        callInfo,
        inferenceComponents
    )
}


fun createSimpleConsumer(
    session: FirSession,
    name: Name,
    token: TowerScopeLevel.Token<*>,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents
): TowerDataConsumer {
    val factory = CandidateFactory(inferenceComponents, callInfo)
    return if (callInfo.explicitReceiver != null) {
        ExplicitReceiverTowerDataConsumer(
            session,
            name,
            token,
            object : ReceiverValueWithPossibleTypes {
                override val type: ConeKotlinType
                    get() = callInfo.typeProvider(callInfo.explicitReceiver)?.coneTypeSafe()
                        ?: ConeKotlinErrorType("No type calculated for: ${callInfo.explicitReceiver.renderWithType()}") // TODO: assert here
            },
            factory
        )
    } else {
        NoExplicitReceiverTowerDataConsumer(session, name, token, factory)
    }
}


class PrioritizedTowerDataConsumer(
    vararg val consumers: TowerDataConsumer
) : TowerDataConsumer() {

    override fun consume(
        kind: TowerDataKind,
        implicitReceiverType: ConeKotlinType?,
        towerScopeLevel: TowerScopeLevel,
        resultCollector: CandidateCollector,
        group: Int
    ): ProcessorAction {
        if (checkSkip(group, resultCollector)) return ProcessorAction.NEXT
        for ((index, consumer) in consumers.withIndex()) {
            val action = consumer.consume(kind, implicitReceiverType, towerScopeLevel, resultCollector, group * consumers.size + index)
            if (action.stop()) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}

class ExplicitReceiverTowerDataConsumer<T : ConeSymbol>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val explicitReceiver: ReceiverValueWithPossibleTypes,
    val candidateFactory: CandidateFactory
) : TowerDataConsumer() {


    override fun consume(
        kind: TowerDataKind,
        implicitReceiverType: ConeKotlinType?,
        towerScopeLevel: TowerScopeLevel,
        resultCollector: CandidateCollector,
        group: Int
    ): ProcessorAction {
        if (checkSkip(group, resultCollector)) return ProcessorAction.NEXT
        return when (kind) {
            TowerDataKind.EMPTY ->
                MemberScopeTowerLevel(session, explicitReceiver).processElementsByName(
                    token,
                    name,
                    extensionReceiver = null,
                    processor = object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction {
                            resultCollector.consumeCandidate(
                                group,
                                candidateFactory.createCandidate(
                                    symbol,
                                    boundDispatchReceiver,
                                    ExplicitReceiverKind.DISPATCH_RECEIVER
                                )
                            )
                            return ProcessorAction.NEXT
                        }
                    }
                )
            TowerDataKind.TOWER_LEVEL -> {
                if (token == TowerScopeLevel.Token.Objects) return ProcessorAction.NEXT
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    extensionReceiver = explicitReceiver,
                    processor = object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction {
                            resultCollector.consumeCandidate(
                                group,
                                candidateFactory.createCandidate(
                                    symbol,
                                    boundDispatchReceiver,
                                    ExplicitReceiverKind.EXTENSION_RECEIVER
                                )
                            )
                            return ProcessorAction.NEXT
                        }
                    }
                )
            }
        }
    }

}

class NoExplicitReceiverTowerDataConsumer<T : ConeSymbol>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val candidateFactory: CandidateFactory
) : TowerDataConsumer() {


    override fun consume(
        kind: TowerDataKind,
        implicitReceiverType: ConeKotlinType?,
        towerScopeLevel: TowerScopeLevel,
        resultCollector: CandidateCollector,
        group: Int
    ): ProcessorAction {
        if (checkSkip(group, resultCollector)) return ProcessorAction.NEXT
        return when (kind) {

            TowerDataKind.TOWER_LEVEL -> {
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    null,
                    object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(symbol: T, boundDispatchReceiver: ReceiverValueWithPossibleTypes?): ProcessorAction {
                            resultCollector.consumeCandidate(
                                group,
                                candidateFactory.createCandidate(symbol, boundDispatchReceiver, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER)
                            )
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

        var group = 0

        towerDataConsumer.consume(TowerDataKind.EMPTY, null, TowerScopeLevel.Empty, collector, group++)

        for (scope in scopes!!) {
            towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, null, ScopeTowerLevel(session, scope), collector, group++)
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


        callInfo.callKind.sequence().forEach {
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


    fun bestCandidates(): List<Candidate> {
        if (groupNumbers.isEmpty()) return emptyList()
        val result = mutableListOf<Candidate>()
        var bestGroup = groupNumbers.first()
        for ((index, candidate) in candidates.withIndex()) {
            val group = groupNumbers[index]
            if (bestGroup > group) {
                bestGroup = group
                result.clear()
            }
            if (bestGroup == group) {
                result.add(candidate)
            }
        }
        return result
    }

    fun isSuccess(): Boolean {
        return currentApplicability == CandidateApplicability.RESOLVED
    }
}

fun FirCallableDeclaration.dispatchReceiverType(session: FirSession): ConeKotlinType? {
    val id = (this.symbol as ConeCallableSymbol).callableId.classId ?: return null
    val symbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(id) as? FirClassSymbol ?: return null
    return symbol.fir.defaultType()
}