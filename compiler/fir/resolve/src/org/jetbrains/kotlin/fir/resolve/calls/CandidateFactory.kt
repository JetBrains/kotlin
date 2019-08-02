/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class CandidateFactory(
    val inferenceComponents: InferenceComponents,
    val callInfo: CallInfo
) {

    val baseSystem: ConstraintStorage

    init {
        val system = inferenceComponents.createConstraintSystem()
        callInfo.explicitReceiver?.let { system.addSubsystemFromExpression(it) }
        callInfo.arguments.forEach {
            system.addSubsystemFromExpression(it)
        }
        baseSystem = system.asReadOnlyStorage()
    }

    fun createCandidate(
        symbol: AbstractFirBasedSymbol<*>,
        dispatchReceiverValue: ClassDispatchReceiverValue?,
        implicitExtensionReceiverValue: ImplicitReceiverValue?,
        explicitReceiverKind: ExplicitReceiverKind
    ): Candidate {
        return Candidate(
            symbol, dispatchReceiverValue, implicitExtensionReceiverValue,
            explicitReceiverKind, inferenceComponents, baseSystem, callInfo
        )
    }
}

fun PostponedArgumentsAnalyzer.Context.addSubsystemFromExpression(expression: FirExpression) {
    when (expression) {
        is FirFunctionCall, is FirCallLikeControlFlowExpression -> (expression as FirResolvable).candidate()?.let { addOtherSystem(it.system.asReadOnlyStorage()) }
        is FirWrappedArgumentExpression -> addSubsystemFromExpression(expression.expression)
        is FirBlock -> expression.returnExpressions().forEach { addSubsystemFromExpression(it) }
    }
}

internal fun FirResolvable.candidate(): Candidate? {
    return when (val callee = this.calleeReference) {
        is FirNamedReferenceWithCandidate -> return callee.candidate
        else -> null
    }
}