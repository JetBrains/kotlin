/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contract.contextual.safeBuilder

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContext
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextVerifier
import org.jetbrains.kotlin.fir.contracts.contextual.diagnostics.CoeffectContextVerificationError
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

class SafeBuilderInitializationRequiredError(
    val target: FirCallableSymbol<*>, val property: FirCallableSymbol<*>, val range: EventOccurrencesRange
) : CoeffectContextVerificationError

class SafeBuilderInvocationRequiredError(
    val target: FirCallableSymbol<*>, val function: FirCallableSymbol<*>, val range: EventOccurrencesRange
) : CoeffectContextVerificationError

class SafeBuilderUnprovidedInitializationError(
    val target: FirCallableSymbol<*>, val property: FirCallableSymbol<*>
) : CoeffectContextVerificationError

class SafeBuilderUnprovidedInvocationError(
    val target: FirCallableSymbol<*>, val function: FirCallableSymbol<*>
) : CoeffectContextVerificationError

object SafeBuilderActionProvidingVerifier : CoeffectContextVerifier {
    override val family = SafeBuilderCoeffectFamily
    override val needVerifyOnCurrentNode: Boolean = true

    override fun verifyContext(context: CoeffectContext, session: FirSession): List<CoeffectContextVerificationError> {
        if (context !is SafeBuilderCoeffectContext) throw AssertionError()
        if (context.actions.isEmpty()) return emptyList()
        val errors = mutableListOf<CoeffectContextVerificationError>()

        for ((action, range) in context.actions) {
            if (range == EventOccurrencesRange.ZERO) continue
            errors += when (action.type) {
                SafeBuilderActionType.INITIALIZATION -> SafeBuilderUnprovidedInitializationError(action.owner, action.member)
                SafeBuilderActionType.INVOCATION -> SafeBuilderUnprovidedInvocationError(action.owner, action.member)
            }
        }

        return errors
    }
}

class SafeBuilderCoeffectContextVerifier(val action: SafeBuilderAction, val requiredKind: EventOccurrencesRange) : CoeffectContextVerifier {
    override val family = SafeBuilderCoeffectFamily

    override fun verifyContext(context: CoeffectContext, session: FirSession): List<CoeffectContextVerificationError> {
        if (context !is SafeBuilderCoeffectContext) throw AssertionError()
        val existingKind = context.actions[action] ?: EventOccurrencesRange.ZERO

        return if (existingKind !in requiredKind) {
            val error = when (action.type) {
                SafeBuilderActionType.INITIALIZATION -> SafeBuilderInitializationRequiredError(action.owner, action.member, requiredKind)
                SafeBuilderActionType.INVOCATION -> SafeBuilderInvocationRequiredError(action.owner, action.member, requiredKind)
            }
            listOf(error)
        } else emptyList()
    }
}