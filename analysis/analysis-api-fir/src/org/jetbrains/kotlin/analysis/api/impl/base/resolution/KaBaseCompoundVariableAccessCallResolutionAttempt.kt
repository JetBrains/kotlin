/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

@KaImplementationDetail
class KaBaseCompoundVariableAccessCallResolutionAttempt(
    backingCompoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    private val backingVariableCallAttempt: KaSimpleCallResolutionAttempt,
    private val backingOperationCallAttempt: KaSimpleCallResolutionAttempt,
) : KaCompoundVariableAccessCallResolutionAttempt {
    private val backingCompoundOperation: KaCompoundOperation? =
        backingOperationCallAttempt.toCompoundOperation(backingCompoundOperationProvider)

    override val token: KaLifetimeToken get() = backingVariableCallAttempt.token

    override val call: KaCompoundVariableAccessCall?
        get() = withValidityAssertion {
            backingCompoundOperation?.let { compoundOperation ->
                assembleMultiCall(backingVariableCallAttempt, backingOperationCallAttempt) { variable, _ ->
                    KaBaseCompoundVariableAccessCall(variable.call as KaVariableAccessCall, compoundOperation)
                }
            }
        }

    override val variableCallAttempt: KaSimpleCallResolutionAttempt get() = withValidityAssertion { backingVariableCallAttempt }
    override val operationCallAttempt: KaSimpleCallResolutionAttempt get() = withValidityAssertion { backingOperationCallAttempt }
    override val simpleAttempts: List<KaSimpleCallResolutionAttempt>
        get() = withValidityAssertion { listOf(backingVariableCallAttempt, backingOperationCallAttempt) }

    @Deprecated("Use 'simpleAttempts' instead", ReplaceWith("simpleAttempts"))
    override val attempts: List<KaSimpleCallResolutionAttempt> get() = simpleAttempts
}
