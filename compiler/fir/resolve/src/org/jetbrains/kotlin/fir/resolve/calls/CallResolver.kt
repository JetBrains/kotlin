/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.constructClassType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorWithJump
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByNameWithAction
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted

class CallInfo(
    val callKind: CallKind,

    val explicitReceiver: FirExpression?,
    val arguments: List<FirExpression>,
    val isSafeCall: Boolean,

    val typeArguments: List<FirTypeProjection>,
    val session: FirSession,
    val containingFile: FirFile,
    val container: FirDeclaration,
    val typeProvider: (FirExpression) -> FirTypeRef?
) {
    val argumentCount get() = arguments.size
}

interface CheckerSink {
    fun reportApplicability(new: CandidateApplicability)
    suspend fun yield()
    suspend fun yieldApplicability(new: CandidateApplicability) {
        reportApplicability(new)
        yield()
    }

    val components: InferenceComponents

    suspend fun yieldIfNeed()
}


class CheckerSinkImpl(override val components: InferenceComponents, var continuation: Continuation<Unit>? = null) : CheckerSink {
    var current = CandidateApplicability.RESOLVED
    override fun reportApplicability(new: CandidateApplicability) {
        if (new < current) current = new
    }

    override suspend fun yield() = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Unit> {
        continuation = it
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
    }

    override suspend fun yieldIfNeed() {
        if (current < CandidateApplicability.SYNTHETIC_RESOLVED) {
            yield()
        }
    }
}


class Candidate(
    val symbol: FirBasedSymbol<*>,
    val dispatchReceiverValue: ClassDispatchReceiverValue?,
    val implicitExtensionReceiverValue: ImplicitReceiverValue?,
    val explicitReceiverKind: ExplicitReceiverKind,
    private val inferenceComponents: InferenceComponents,
    private val baseSystem: ConstraintStorage,
    val callInfo: CallInfo
) {
    val system by lazy {
        val system = inferenceComponents.createConstraintSystem()
        system.addOtherSystem(baseSystem)
        system
    }
    lateinit var substitutor: ConeSubstitutor

    var argumentMapping: Map<FirExpression, FirValueParameter>? = null
    val postponedAtoms = mutableListOf<PostponedResolvedAtomMarker>()
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

    sealed class Token<out T : FirBasedSymbol<*>> {
        object Properties : Token<FirPropertySymbol>()

        object Functions : Token<FirFunctionSymbol<*>>()
        object Objects : Token<FirBasedSymbol<*>>()
    }

    fun <T : FirBasedSymbol<*>> processElementsByName(
        token: Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevelProcessor<T>
    ): ProcessorAction

    interface TowerScopeLevelProcessor<T : ConeSymbol> {
        fun consumeCandidate(
            symbol: T,
            dispatchReceiverValue: ClassDispatchReceiverValue?,
            implicitExtensionReceiverValue: ImplicitReceiverValue?
        ): ProcessorAction
    }

    object Empty : TowerScopeLevel {
        override fun <T : FirBasedSymbol<*>> processElementsByName(
            token: Token<T>,
            name: Name,
            explicitReceiver: ExpressionReceiverValue?,
            processor: TowerScopeLevelProcessor<T>
        ): ProcessorAction = ProcessorAction.NEXT
    }
}

abstract class SessionBasedTowerLevel(val session: FirSession) : TowerScopeLevel {
    protected fun FirBasedSymbol<*>.dispatchReceiverValue(): ClassDispatchReceiverValue? {
        return when (this) {
            is FirNamedFunctionSymbol -> fir.dispatchReceiverValue(session)
            is FirClassSymbol -> ClassDispatchReceiverValue(fir.symbol)
            else -> null
        }
    }

    protected fun FirCallableSymbol<*>.hasConsistentExtensionReceiver(extensionReceiver: ReceiverValue?): Boolean {
        val hasExtensionReceiver = hasExtensionReceiver()
        return hasExtensionReceiver == (extensionReceiver != null)
    }
}

// This is more like "dispatch receiver-based tower level"
// Here we always have an explicit or implicit dispatch receiver, and can access members of its scope
// (which is separated from currently accessible scope, see below)
// So: dispatch receiver = given explicit or implicit receiver (always present)
// So: extension receiver = either none, if dispatch receiver = explicit receiver,
//     or given implicit or explicit receiver, otherwise
class MemberScopeTowerLevel(
    session: FirSession,
    val dispatchReceiver: ReceiverValue,
    val implicitExtensionReceiver: ImplicitReceiverValue? = null,
    val scopeSession: ScopeSession
) : SessionBasedTowerLevel(session) {

    private fun <T : FirBasedSymbol<*>> processMembers(
        output: TowerScopeLevel.TowerScopeLevelProcessor<T>,
        explicitExtensionReceiver: ExpressionReceiverValue?,
        processScopeMembers: FirScope.(processor: (T) -> ProcessorAction) -> ProcessorAction
    ): ProcessorAction {
        if (implicitExtensionReceiver != null && explicitExtensionReceiver != null) return ProcessorAction.NEXT
        val extensionReceiver = implicitExtensionReceiver ?: explicitExtensionReceiver
        val scope = dispatchReceiver.type.scope(session, scopeSession) ?: return ProcessorAction.NEXT
        if (scope.processScopeMembers { candidate ->
                if (candidate is FirCallableSymbol<*> && candidate.hasConsistentExtensionReceiver(extensionReceiver)) {
                    // NB: we do not check dispatchReceiverValue != null here,
                    // because of objects & constructors (see comments in dispatchReceiverValue() implementation)
                    output.consumeCandidate(candidate, candidate.dispatchReceiverValue(), implicitExtensionReceiver)
                } else if (candidate is FirClassLikeSymbol<*>) {
                    output.consumeCandidate(candidate, null, implicitExtensionReceiver)
                } else {
                    ProcessorAction.NEXT
                }
            }.stop()
        ) return ProcessorAction.STOP
        val withSynthetic = FirSyntheticPropertiesScope(session, scope, ReturnTypeCalculatorWithJump(session, scopeSession))
        return withSynthetic.processScopeMembers { symbol ->
            output.consumeCandidate(symbol, symbol.dispatchReceiverValue(), implicitExtensionReceiver)
        }
    }

    override fun <T : FirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        val explicitExtensionReceiver = if (dispatchReceiver == explicitReceiver) null else explicitReceiver
        return when (token) {
            TowerScopeLevel.Token.Properties -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processPropertiesByName(name, symbol.cast())
            }
            TowerScopeLevel.Token.Functions -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processFunctionsByName(name, symbol.cast())
            }
            TowerScopeLevel.Token.Objects -> processMembers(processor, explicitExtensionReceiver) { symbol ->
                this.processClassifiersByNameWithAction(name, FirPosition.OTHER, symbol.cast())
            }
        }
    }

}

private fun FirCallableSymbol<*>.hasExtensionReceiver(): Boolean = this.fir.receiverTypeRef != null

// This is more like "scope-based tower level"
// We can access here members of currently accessible scope which is not influenced by explicit receiver
// We can either have no explicit receiver at all, or it can be an extension receiver
// An explicit receiver never can be a dispatch receiver at this level
// So: dispatch receiver = strictly NONE
// So: extension receiver = either none or explicit
// (if explicit receiver exists, it always *should* be an extension receiver)
class ScopeTowerLevel(
    session: FirSession,
    val scope: FirScope,
    val implicitExtensionReceiver: ImplicitReceiverValue? = null
) : SessionBasedTowerLevel(session) {
    override fun <T : FirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        if (explicitReceiver != null && implicitExtensionReceiver != null) {
            return ProcessorAction.NEXT
        }
        val extensionReceiver = explicitReceiver ?: implicitExtensionReceiver
        return when (token) {

            TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(name) { candidate ->
                if (candidate.hasConsistentExtensionReceiver(extensionReceiver) && candidate.dispatchReceiverValue() == null) {
                    processor.consumeCandidate(
                        candidate as T, dispatchReceiverValue = null,
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                } else {
                    ProcessorAction.NEXT
                }
            }
            TowerScopeLevel.Token.Functions -> scope.processFunctionsByName(name) { candidate ->
                if (candidate.hasConsistentExtensionReceiver(extensionReceiver) && candidate.dispatchReceiverValue() == null) {
                    processor.consumeCandidate(
                        candidate as T, dispatchReceiverValue = null,
                        implicitExtensionReceiverValue = implicitExtensionReceiver
                    )
                } else {
                    ProcessorAction.NEXT
                }
            }
            TowerScopeLevel.Token.Objects -> scope.processClassifiersByNameWithAction(name, FirPosition.OTHER) {
                processor.consumeCandidate(
                    it as T, dispatchReceiverValue = null,
                    implicitExtensionReceiverValue = null
                )
            }
        }
    }

}

/**
 *  Handles only statics and top-levels, DOES NOT handle objects/companions members
 */
class QualifiedReceiverTowerLevel(session: FirSession) : SessionBasedTowerLevel(session) {
    override fun <T : FirBasedSymbol<*>> processElementsByName(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        explicitReceiver: ExpressionReceiverValue?,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        val qualifiedReceiver = explicitReceiver?.explicitReceiverExpression as FirResolvedQualifier
        val scope = FirExplicitSimpleImportingScope(
            listOf(
                FirResolvedImportImpl(
                    session,
                    FirImportImpl(session, null, FqName.topLevel(name), false, null),
                    qualifiedReceiver.packageFqName,
                    qualifiedReceiver.relativeClassFqName
                )
            ), session
        )

        return if (token == TowerScopeLevel.Token.Objects) {
            scope.processClassifiersByNameWithAction(name, FirPosition.OTHER) {
                processor.consumeCandidate(it as T, null, null)
            }
        } else {
            scope.processCallables(name, token.cast()) {
                val fir = it.fir
                if (fir is FirCallableMemberDeclaration<*> && fir.isStatic || it.callableId.classId == null) {
                    processor.consumeCandidate(it as T, null, null)
                } else {
                    ProcessorAction.NEXT
                }
            }
        }
    }

}

class QualifiedReceiverTowerDataConsumer<T : FirBasedSymbol<*>>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val explicitReceiver: ExpressionReceiverValue,
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector
) : TowerDataConsumer() {
    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        if (kind != TowerDataKind.EMPTY) return ProcessorAction.NEXT

        return QualifiedReceiverTowerLevel(session).processElementsByName(
            token,
            name,
            explicitReceiver,
            processor = object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                override fun consumeCandidate(
                    symbol: T,
                    dispatchReceiverValue: ClassDispatchReceiverValue?,
                    implicitExtensionReceiverValue: ImplicitReceiverValue?
                ): ProcessorAction {
                    assert(dispatchReceiverValue == null)
                    resultCollector.consumeCandidate(
                        group,
                        candidateFactory.createCandidate(
                            symbol,
                            dispatchReceiverValue = null,
                            implicitExtensionReceiverValue = null,
                            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                        )
                    )
                    return ProcessorAction.NEXT
                }
            }
        )
    }
}


abstract class TowerDataConsumer {
    abstract fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
//        resultCollector: CandidateCollector,
        group: Int
    ): ProcessorAction

    private var stopGroup = Int.MAX_VALUE
    fun skipGroup(group: Int, resultCollector: CandidateCollector): Boolean {
        if (resultCollector.isSuccess() && stopGroup == Int.MAX_VALUE) {
            stopGroup = group
        } else if (group > stopGroup) return true
        return false
    }
}


fun createVariableAndObjectConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    return PrioritizedTowerDataConsumer(
        resultCollector,
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Properties,
            callInfo,
            inferenceComponents,
            resultCollector
        ),
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Objects,
            callInfo,
            inferenceComponents,
            resultCollector
        )
    )

}

fun createSimpleFunctionConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    return createSimpleConsumer(
        session,
        name,
        TowerScopeLevel.Token.Functions,
        callInfo,
        inferenceComponents,
        resultCollector
    )
}

fun createFunctionConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents,
    resultCollector: CandidateCollector,
    callResolver: CallResolver
): TowerDataConsumer {
    val varCallInfo = CallInfo(
        CallKind.VariableAccess,
        callInfo.explicitReceiver,
        emptyList(),
        callInfo.isSafeCall,
        callInfo.typeArguments,
        inferenceComponents.session,
        callInfo.containingFile,
        callInfo.container,
        callInfo.typeProvider
    )
    return PrioritizedTowerDataConsumer(
        resultCollector,
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Functions,
            callInfo,
            inferenceComponents,
            resultCollector
        ),
        AccumulatingTowerDataConsumer(resultCollector).apply {
            initialConsumer = createSimpleConsumer(
                session,
                name,
                TowerScopeLevel.Token.Properties,
                varCallInfo,
                inferenceComponents,
                InvokeReceiverCandidateCollector(
                    callResolver,
                    invokeCallInfo = callInfo,
                    components = inferenceComponents,
                    invokeConsumer = this
                )
            )
        }
    )
}


fun createSimpleConsumer(
    session: FirSession,
    name: Name,
    token: TowerScopeLevel.Token<*>,
    callInfo: CallInfo,
    inferenceComponents: InferenceComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    val factory = CandidateFactory(inferenceComponents, callInfo)
    val explicitReceiver = callInfo.explicitReceiver
    return if (explicitReceiver != null) {
        val receiverValue = ExpressionReceiverValue(explicitReceiver, callInfo.typeProvider)
        if (explicitReceiver is FirResolvedQualifier) {
            val qualified =
                QualifiedReceiverTowerDataConsumer(session, name, token, receiverValue, factory, resultCollector)

            if (explicitReceiver.classId != null) {
                PrioritizedTowerDataConsumer(
                    resultCollector,
                    qualified,
                    ExplicitReceiverTowerDataConsumer(session, name, token, receiverValue, factory, resultCollector)
                )
            } else {
                qualified
            }

        } else {
            ExplicitReceiverTowerDataConsumer(session, name, token, receiverValue, factory, resultCollector)
        }
    } else {
        NoExplicitReceiverTowerDataConsumer(session, name, token, factory, resultCollector)
    }
}


class PrioritizedTowerDataConsumer(
    val resultCollector: CandidateCollector,
    vararg val consumers: TowerDataConsumer
) : TowerDataConsumer() {

    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        for ((index, consumer) in consumers.withIndex()) {
            val action = consumer.consume(kind, towerScopeLevel, group * consumers.size + index)
            if (action.stop()) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}

// Yet is used exclusively for invokes:
// - initialConsumer consumes property which is invoke receiver
// - additionalConsumers consume invoke calls themselves
class AccumulatingTowerDataConsumer(
    private val resultCollector: CandidateCollector
) : TowerDataConsumer() {

    lateinit var initialConsumer: TowerDataConsumer
    private val additionalConsumers = mutableListOf<TowerDataConsumer>()

    private data class TowerData(val kind: TowerDataKind, val level: TowerScopeLevel, val group: Int)

    private val accumulatedTowerData = mutableListOf<TowerData>()

    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        accumulatedTowerData += TowerData(kind, towerScopeLevel, group)

        if (initialConsumer.consume(kind, towerScopeLevel, group).stop()) {
            return ProcessorAction.STOP
        }
        for (consumer in additionalConsumers) {
            val action = consumer.consume(kind, towerScopeLevel, group)
            if (action.stop()) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }

    fun addConsumer(consumer: TowerDataConsumer): ProcessorAction {
        additionalConsumers += consumer
        for ((kind, level, group) in accumulatedTowerData) {
            if (consumer.consume(kind, level, group).stop()) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}

class ExplicitReceiverTowerDataConsumer<T : FirBasedSymbol<*>>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val explicitReceiver: ExpressionReceiverValue,
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector
) : TowerDataConsumer() {

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }


    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        return when (kind) {
            TowerDataKind.EMPTY ->
                MemberScopeTowerLevel(session, explicitReceiver, scopeSession = candidateFactory.inferenceComponents.scopeSession)
                    .processElementsByName(
                        token,
                        name,
                        explicitReceiver = null,
                        processor = object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                            override fun consumeCandidate(
                                symbol: T,
                                dispatchReceiverValue: ClassDispatchReceiverValue?,
                                implicitExtensionReceiverValue: ImplicitReceiverValue?
                            ): ProcessorAction {
                                resultCollector.consumeCandidate(
                                    group,
                                    candidateFactory.createCandidate(
                                        symbol,
                                        dispatchReceiverValue,
                                        implicitExtensionReceiverValue,
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
                    explicitReceiver = explicitReceiver,
                    processor = object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(
                            symbol: T,
                            dispatchReceiverValue: ClassDispatchReceiverValue?,
                            implicitExtensionReceiverValue: ImplicitReceiverValue?
                        ): ProcessorAction {
                            if (symbol is FirNamedFunctionSymbol && symbol.callableId.packageName.startsWith(defaultPackage)) {
                                val explicitReceiverType = explicitReceiver.type
                                if (dispatchReceiverValue == null && explicitReceiverType is ConeClassType) {
                                    val declarationReceiverTypeRef =
                                        (symbol as? FirCallableSymbol<*>)?.fir?.receiverTypeRef as? FirResolvedTypeRef
                                    val declarationReceiverType = declarationReceiverTypeRef?.type
                                    if (declarationReceiverType is ConeClassType) {
                                        if (!AbstractTypeChecker.isSubtypeOf(
                                                candidateFactory.inferenceComponents.ctx,
                                                explicitReceiverType,
                                                declarationReceiverType.lookupTag.constructClassType(
                                                    declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
                                                    isNullable = true
                                                )
                                            )
                                        ) {
                                            return ProcessorAction.NEXT
                                        }
                                    }
                                }
                            }
                            val candidate = candidateFactory.createCandidate(
                                symbol,
                                dispatchReceiverValue,
                                implicitExtensionReceiverValue,
                                ExplicitReceiverKind.EXTENSION_RECEIVER
                            )

                            resultCollector.consumeCandidate(
                                group,
                                candidate
                            )
                            return ProcessorAction.NEXT
                        }
                    }
                )
            }
        }
    }

}

class NoExplicitReceiverTowerDataConsumer<T : FirBasedSymbol<*>>(
    val session: FirSession,
    val name: Name,
    val token: TowerScopeLevel.Token<T>,
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector
) : TowerDataConsumer() {


    override fun consume(
        kind: TowerDataKind,
        towerScopeLevel: TowerScopeLevel,
        group: Int
    ): ProcessorAction {
        if (skipGroup(group, resultCollector)) return ProcessorAction.NEXT
        return when (kind) {

            TowerDataKind.TOWER_LEVEL -> {
                towerScopeLevel.processElementsByName(
                    token,
                    name,
                    explicitReceiver = null,
                    processor = object : TowerScopeLevel.TowerScopeLevelProcessor<T> {
                        override fun consumeCandidate(
                            symbol: T,
                            dispatchReceiverValue: ClassDispatchReceiverValue?,
                            implicitExtensionReceiverValue: ImplicitReceiverValue?
                        ): ProcessorAction {
                            resultCollector.consumeCandidate(
                                group,
                                candidateFactory.createCandidate(
                                    symbol,
                                    dispatchReceiverValue,
                                    implicitExtensionReceiverValue,
                                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                                )
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

class CallResolver(val typeCalculator: ReturnTypeCalculator, val components: InferenceComponents) {

    var callInfo: CallInfo? = null

    var scopes: List<FirScope>? = null

    val session: FirSession get() = components.session

    private fun processImplicitReceiver(
        towerDataConsumer: TowerDataConsumer,
        implicitReceiverValue: ImplicitReceiverValue,
        collector: CandidateCollector,
        oldGroup: Int
    ): Int {
        var group = oldGroup
        towerDataConsumer.consume(
            TowerDataKind.TOWER_LEVEL,
            MemberScopeTowerLevel(session, implicitReceiverValue, scopeSession = components.scopeSession),
            group++
        )

        // This is an equivalent to the old "BothTowerLevelAndImplicitReceiver"
        towerDataConsumer.consume(
            TowerDataKind.TOWER_LEVEL,
            MemberScopeTowerLevel(session, implicitReceiverValue, implicitReceiverValue, components.scopeSession),
            group++
        )

        for (scope in scopes!!) {
            towerDataConsumer.consume(
                TowerDataKind.TOWER_LEVEL,
                ScopeTowerLevel(session, scope, implicitReceiverValue),
                group++
            )
        }

        return group
    }

    val collector by lazy { CandidateCollector(components) }
    lateinit var towerDataConsumer: TowerDataConsumer
    private lateinit var implicitReceiverValues: List<ImplicitReceiverValue>

    fun runTowerResolver(consumer: TowerDataConsumer, implicitReceiverValues: List<ImplicitReceiverValue>): CandidateCollector {
        this.implicitReceiverValues = implicitReceiverValues
        towerDataConsumer = consumer

        var group = 0

        towerDataConsumer.consume(TowerDataKind.EMPTY, TowerScopeLevel.Empty, group++)

        for (scope in scopes!!) {
            towerDataConsumer.consume(TowerDataKind.TOWER_LEVEL, ScopeTowerLevel(session, scope), group++)
        }

        var blockDispatchReceivers = false
        for (implicitReceiverValue in implicitReceiverValues) {
            if (implicitReceiverValue is ImplicitDispatchReceiverValue) {
                if (blockDispatchReceivers) {
                    continue
                }
                if (!implicitReceiverValue.boundSymbol.fir.isInner) {
                    blockDispatchReceivers = true
                }
            }
            group = processImplicitReceiver(towerDataConsumer, implicitReceiverValue, collector, group)
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

open class CandidateCollector(val components: InferenceComponents) {

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

        val sink = CheckerSinkImpl(components)
        var finished = false
        sink.continuation = suspend {
            for (stage in candidate.callInfo.callKind.sequence()) {
                stage.check(candidate, sink, candidate.callInfo)
            }
        }.createCoroutineUnintercepted(completion = object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                result.exceptionOrNull()?.let { throw it }
                finished = true
            }
        })




        while (!finished) {
            sink.continuation!!.resume(Unit)
            if (sink.current < CandidateApplicability.SYNTHETIC_RESOLVED) {
                break
            }
        }
        return sink.current
    }

    open fun consumeCandidate(group: Int, candidate: Candidate): CandidateApplicability {
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

        return applicability
    }


    fun bestCandidates(): List<Candidate> {
        if (groupNumbers.isEmpty()) return emptyList()
        if (groupNumbers.size == 1) return candidates
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

// Collects properties that potentially could be invoke receivers, like 'propertyName()',
// and initiates further invoke resolution by adding property-bound invoke consumers
class InvokeReceiverCandidateCollector(
    val callResolver: CallResolver,
    val invokeCallInfo: CallInfo,
    components: InferenceComponents,
    val invokeConsumer: AccumulatingTowerDataConsumer
) : CandidateCollector(components) {
    override fun consumeCandidate(group: Int, candidate: Candidate): CandidateApplicability {
        val applicability = super.consumeCandidate(group, candidate)

        if (applicability >= CandidateApplicability.SYNTHETIC_RESOLVED) {

            val session = components.session
            val boundInvokeCallInfo = CallInfo(
                invokeCallInfo.callKind,
                FirQualifiedAccessExpressionImpl(session, null, false).apply {
                    calleeReference = FirNamedReferenceWithCandidate(
                        session,
                        null,
                        (candidate.symbol as ConeCallableSymbol).callableId.callableName,
                        candidate
                    )
                    typeRef = callResolver.typeCalculator.tryCalculateReturnType(candidate.symbol.firUnsafe())
                },
                invokeCallInfo.arguments,
                invokeCallInfo.isSafeCall,
                invokeCallInfo.typeArguments,
                session,
                invokeCallInfo.containingFile,
                invokeCallInfo.container,
                invokeCallInfo.typeProvider
            )

            invokeConsumer.addConsumer(
                createSimpleFunctionConsumer(
                    session, OperatorNameConventions.INVOKE,
                    boundInvokeCallInfo, components, callResolver.collector
                )
            )
        }

        return applicability
    }
}

fun FirCallableDeclaration<*>.dispatchReceiverValue(session: FirSession): ClassDispatchReceiverValue? {
    // TODO: this is not true at least for inner class constructors
    if (this is FirConstructor) return null
    val id = (this.symbol as ConeCallableSymbol).callableId.classId ?: return null
    val symbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(id) as? FirClassSymbol ?: return null
    val regularClass = symbol.fir

    // TODO: this is also not true, but objects can be also imported
    if (regularClass.classKind == ClassKind.OBJECT) return null

    return ClassDispatchReceiverValue(regularClass.symbol)
}
