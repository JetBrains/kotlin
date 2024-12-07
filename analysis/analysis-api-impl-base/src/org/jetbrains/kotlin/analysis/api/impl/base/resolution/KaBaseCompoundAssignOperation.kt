/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundAssignOperation
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseCompoundAssignOperation(
    private val backingOperationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>,
    kind: KaCompoundAssignOperation.Kind,
    operand: KtExpression,
) : KaCompoundAssignOperation {
    override val token: KaLifetimeToken get() = backingOperationPartiallyAppliedSymbol.token

    override val kind: KaCompoundAssignOperation.Kind by validityAsserted(kind)
    override val operand: KtExpression by validityAsserted(operand)
    override val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
        get() = withValidityAssertion { backingOperationPartiallyAppliedSymbol }
}
