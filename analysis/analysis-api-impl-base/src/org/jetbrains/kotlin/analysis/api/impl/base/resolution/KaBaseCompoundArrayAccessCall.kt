/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
class KaBaseCompoundArrayAccessCall(
    private val backingCompoundAccess: KaCompoundOperation,
    private val backingIndexArguments: List<KtExpression>,
    private val backingGetCall: KaFunctionCall<KaNamedFunctionSymbol>,
    private val backingSetCall: KaFunctionCall<KaNamedFunctionSymbol>,
) : KaCompoundArrayAccessCall, KaCompoundAccessCall {
    override val token: KaLifetimeToken get() = backingCompoundAccess.token

    override val compoundOperation: KaCompoundOperation
        get() = withValidityAssertion { backingCompoundAccess }

    override val indexArguments: List<KtExpression>
        get() = withValidityAssertion { backingIndexArguments }

    @Deprecated("Use 'getCall' instead")
    override val getPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
        get() = withValidityAssertion { getCall.partiallyAppliedSymbol }

    override val getCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingGetCall }

    @Deprecated("Use 'setCall' instead")
    override val setPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
        get() = withValidityAssertion { setCall.partiallyAppliedSymbol }

    override val setCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingSetCall }
}
