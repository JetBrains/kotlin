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
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseCompoundArrayAccessCallResolutionAttempt(
    private val backingCompoundOperation: KaCompoundOperation?,
    private val backingIndexArguments: List<KtExpression>,
    private val backingGetterCallAttempt: KaSingleCallResolutionAttempt,
    private val backingOperationCallAttempt: KaSingleCallResolutionAttempt,
    private val backingSetterCallAttempt: KaSingleCallResolutionAttempt,
) : KaCompoundArrayAccessCallResolutionAttempt {
    override val token: KaLifetimeToken get() = backingGetterCallAttempt.token

    @Suppress("UNCHECKED_CAST")
    override val call: KaCompoundArrayAccessCall?
        get() = withValidityAssertion {
            if (backingCompoundOperation != null) {
                assembleMultiCall(backingGetterCallAttempt, backingOperationCallAttempt, backingSetterCallAttempt) { getter, _, setter ->
                    KaBaseCompoundArrayAccessCall(
                        backingCompoundOperation,
                        backingIndexArguments,
                        getter.call as KaFunctionCall<KaNamedFunctionSymbol>,
                        setter.call as KaFunctionCall<KaNamedFunctionSymbol>,
                    )
                }
            } else {
                null
            }
        }

    override val getterCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingGetterCallAttempt }
    override val operationCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingOperationCallAttempt }
    override val setterCallAttempt: KaSingleCallResolutionAttempt get() = withValidityAssertion { backingSetterCallAttempt }
    override val attempts: List<KaSingleCallResolutionAttempt>
        get() = withValidityAssertion { listOf(backingGetterCallAttempt, backingOperationCallAttempt, backingSetterCallAttempt) }
}
