/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol

@KaImplementationDetail
class KaBaseCompoundVariableAccessCall(
    private val backingVariableCall: KaVariableAccessCall,
    private val backingCompoundOperation: KaCompoundOperation,
) : KaCompoundVariableAccessCall {
    override val token: KaLifetimeToken get() = backingVariableCall.token

    @Deprecated("Use 'variableCall' instead")
    override val variablePartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>
        get() = withValidityAssertion { backingVariableCall.asPartiallyAppliedSymbol }

    override val variableCall: KaVariableAccessCall
        get() = withValidityAssertion { backingVariableCall }

    override val compoundOperation: KaCompoundOperation
        get() = withValidityAssertion { backingCompoundOperation }

    @KaExperimentalApi
    override val calls: List<KaSingleCall<*, *>>
        get() = withValidityAssertion {
            listOf(backingVariableCall, backingCompoundOperation.operationCall)
        }
}
