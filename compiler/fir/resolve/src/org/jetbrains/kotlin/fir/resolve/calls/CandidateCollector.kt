/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

open class CandidateCollector(val components: InferenceComponents) {

    val groupNumbers = mutableListOf<Int>()
    val candidates = mutableListOf<Candidate>()

    var currentApplicability = CandidateApplicability.HIDDEN

    fun newDataSet() {
        groupNumbers.clear()
        candidates.clear()
        currentApplicability = CandidateApplicability.HIDDEN
    }

    private fun getApplicability(
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
