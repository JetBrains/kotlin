/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*

@KaImplementationDetail
class KaBaseCompoundVariableAccessCallResolutionAttempt(
    private val backingCompoundOperation: KaCompoundOperation?,
    private val backingVariableCallAttempt: KaSingleCallResolutionAttempt,
    private val backingOperationCallAttempt: KaSingleCallResolutionAttempt,
) : KaCompoundVariableAccessCallResolutionAttempt {
    override val token: KaLifetimeToken get() = backingVariableCallAttempt.token

    override val call: KaCompoundVariableAccessCall?
        get() = withValidityAssertion {
            if (backingCompoundOperation != null) {
                assembleMultiCall(backingVariableCallAttempt, backingOperationCallAttempt) { variable, _ ->
                    KaBaseCompoundVariableAccessCall(variable.call as KaVariableAccessCall, backingCompoundOperation)
                }
            } else {
                null
            }
        }

    override val variableCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingVariableCallAttempt }
    override val operationCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingOperationCallAttempt }
    override val attempts: List<KaSingleCallResolutionAttempt>
        get() = withValidityAssertion { listOf(backingVariableCallAttempt, backingOperationCallAttempt) }
}
