/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundUnaryOperation
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol

@KaImplementationDetail
class KaBaseCompoundUnaryOperation(
    private val backingOperationCall: KaFunctionCall<KaNamedFunctionSymbol>,
    private val backingKind: KaCompoundUnaryOperation.Kind,
    private val backingPrecedence: KaCompoundUnaryOperation.Precedence,
) : KaCompoundUnaryOperation {
    override val token: KaLifetimeToken get() = backingOperationCall.token

    override val kind: KaCompoundUnaryOperation.Kind
        get() = withValidityAssertion { backingKind }

    override val precedence: KaCompoundUnaryOperation.Precedence
        get() = withValidityAssertion { backingPrecedence }

    @Deprecated("Use 'operationCall' instead")
    override val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingOperationCall.partiallyAppliedSymbol }

    override val operationCall: KaFunctionCall<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingOperationCall }
}
