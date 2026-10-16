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
    backingCompoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    private val backingIndexArguments: List<KtExpression>,
    private val backingGetterCallAttempt: KaSimpleCallResolutionAttempt,
    private val backingOperationCallAttempt: KaSimpleCallResolutionAttempt,
    private val backingSetterCallAttempt: KaSimpleCallResolutionAttempt,
) : KaCompoundArrayAccessCallResolutionAttempt {
    private val backingCompoundOperation: KaCompoundOperation? =
        backingOperationCallAttempt.toCompoundOperation(backingCompoundOperationProvider)

    override val token: KaLifetimeToken get() = backingGetterCallAttempt.token

    @Suppress("UNCHECKED_CAST")
    override val call: KaCompoundArrayAccessCall?
        get() = withValidityAssertion {
            backingCompoundOperation?.let { compoundOperation ->
                assembleMultiCall(
                    backingGetterCallAttempt,
                    backingOperationCallAttempt,
                    backingSetterCallAttempt,
                ) { getter, _, setter ->
                    KaBaseCompoundArrayAccessCall(
                        compoundOperation,
                        backingIndexArguments,
                        getter.call as KaFunctionCall<KaNamedFunctionSymbol>,
                        setter.call as KaFunctionCall<KaNamedFunctionSymbol>,
                    )
                }
            }
        }

    override val getterCallAttempt: KaSimpleCallResolutionAttempt get() = withValidityAssertion { backingGetterCallAttempt }
    override val operationCallAttempt: KaSimpleCallResolutionAttempt get() = withValidityAssertion { backingOperationCallAttempt }
    override val setterCallAttempt: KaSimpleCallResolutionAttempt get() = withValidityAssertion { backingSetterCallAttempt }
    override val simpleAttempts: List<KaSimpleCallResolutionAttempt>
        get() = withValidityAssertion { listOf(backingGetterCallAttempt, backingOperationCallAttempt, backingSetterCallAttempt) }

    @Deprecated("Use 'simpleAttempts' instead", ReplaceWith("simpleAttempts"))
    override val attempts: List<KaSimpleCallResolutionAttempt> get() = simpleAttempts
}
