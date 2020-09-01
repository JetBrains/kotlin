/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.safeBuilder

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.contracts.contextual.*
import org.jetbrains.kotlin.fir.contracts.contextual.declaration.coeffectActionExtractors
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

object SafeBuilderCoeffectFamily : CoeffectFamily {
    override val id = "Safe Builders"
    override val emptyContext = SafeBuilderCoeffectContext(persistentHashMapOf())
    override val combiner = SafeBuilderCoeffectContextCombiner
}

enum class SafeBuilderActionType {
    INITIALIZATION,
    INVOCATION
}

data class SafeBuilderAction(val owner: FirCallableSymbol<*>, val member: FirCallableSymbol<*>, val type: SafeBuilderActionType)
data class SafeBuilderCoeffectContext(val actions: PersistentMap<SafeBuilderAction, EventOccurrencesRange>) : CoeffectContext

object SafeBuilderCoeffectContextCombiner : CoeffectContextCombiner {
    override fun merge(left: CoeffectContext, right: CoeffectContext): CoeffectContext {
        if (left !is SafeBuilderCoeffectContext || right !is SafeBuilderCoeffectContext) throw AssertionError()
        val actions = mutableMapOf<SafeBuilderAction, EventOccurrencesRange>()
        for (key in left.actions.keys + right.actions.keys) {
            val kind1 = left.actions[key] ?: EventOccurrencesRange.ZERO
            val kind2 = right.actions[key] ?: EventOccurrencesRange.ZERO
            actions[key] = kind1 or kind2
        }
        return SafeBuilderCoeffectContext(actions.toPersistentHashMap())
    }
}

fun ConeActionDeclaration.toSafeBuilderAction(function: FirFunction<*>, targetParameterIndex: Int): SafeBuilderAction? {
    val targetSymbol = if (targetParameterIndex == -1) function.symbol else function.valueParameters.getOrNull(targetParameterIndex)?.symbol
    return toSafeBuilderAction(targetSymbol)
}

fun ConeActionDeclaration.toSafeBuilderAction(targetSymbol: FirCallableSymbol<*>?): SafeBuilderAction? {
    if (targetSymbol == null) return null
    return when (this) {
        is ConePropertyInitializationAction -> SafeBuilderAction(targetSymbol, property, SafeBuilderActionType.INITIALIZATION)
        is ConeFunctionInvocationAction -> SafeBuilderAction(targetSymbol, function, SafeBuilderActionType.INVOCATION)
        else -> null
    }
}

internal fun FirFunctionCall.getTargetSymbol(target: ConeTargetReference): FirCallableSymbol<*>? {
    val valueParameter = target as? ConeValueParameterReference ?: return null

    return if (valueParameter.parameterIndex != -1) {
        val function = toResolvedCallableSymbol()?.fir as? FirFunction<*>
        val targetParameter = function?.valueParameters?.getOrNull(valueParameter.parameterIndex) ?: return null
        argumentMapping?.entries?.find { it.value == targetParameter }?.value?.symbol
    } else when (val receiver = extensionReceiver) {
        is FirThisReceiverExpression -> receiver.calleeReference.boundSymbol as? FirCallableSymbol<*>
        is FirResolvable -> receiver.toResolvedCallableSymbol()
        else -> null
    }
}

fun mustDoEffectCoeffectExtractors(action: ConeActionDeclaration) = coeffectActionExtractors {
    family = SafeBuilderCoeffectFamily

    onOwnerCall {
        val target = action.target as? ConeLambdaArgumentReference ?: return@onOwnerCall noActions()
        val mapping = createArgumentsMapping(it) ?: return@onOwnerCall noActions()
        val targetSymbol = mapping[target.parameter.parameterIndex + 1]?.toResolvedCallableSymbol() ?: return@onOwnerCall noActions()
        val safeBuilderAction = action.toSafeBuilderAction(targetSymbol) ?: return@onOwnerCall noActions()
        actions {
            providers += SafeBuilderCoeffectContextProvider(safeBuilderAction, action.kind)
        }
    }

    onOwnerExit {
        val target = action.target as? ConeLambdaArgumentReference ?: return@onOwnerExit noActions()
        val safeBuilderAction = action.toSafeBuilderAction(it, target.parameter.parameterIndex) ?: return@onOwnerExit noActions()
        actions {
            verifiers += SafeBuilderCoeffectContextVerifier(safeBuilderAction, action.kind)
            cleaners += SafeBuilderCoeffectContextCleaner(safeBuilderAction)
        }
    }
}

fun providesEffectCoeffectExtractors(action: ConeActionDeclaration) = coeffectActionExtractors {
    family = SafeBuilderCoeffectFamily

    onOwnerCall { functionCall ->
        val targetSymbol = functionCall.getTargetSymbol(action.target)
        val safeBuilderAction = action.toSafeBuilderAction(targetSymbol) ?: return@onOwnerCall noActions()
        actions {
            providers += SafeBuilderCoeffectContextProvider(safeBuilderAction, action.kind)
        }
    }

    onOwnerExit {
        val targetSymbol = action.target as? ConeValueParameterReference ?: return@onOwnerExit noActions()
        val safeBuilderAction = action.toSafeBuilderAction(it, targetSymbol.parameterIndex) ?: return@onOwnerExit noActions()
        actions {
            verifiers += SafeBuilderCoeffectContextVerifier(safeBuilderAction, action.kind)
            cleaners += SafeBuilderCoeffectContextCleaner(safeBuilderAction)
        }
    }
}

fun requiresEffectCoeffectExtractors(action: ConeActionDeclaration) = coeffectActionExtractors {
    family = SafeBuilderCoeffectFamily

    onOwnerCall { functionCall ->
        val targetSymbol = functionCall.getTargetSymbol(action.target)
        val safeBuilderAction = action.toSafeBuilderAction(targetSymbol) ?: return@onOwnerCall noActions()
        actions {
            verifiers += SafeBuilderCoeffectContextVerifier(safeBuilderAction, action.kind)
        }
    }
}