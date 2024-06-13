/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.psi.KtExpression

/**
 * The type of access to a variable or using the array access convention.
 */
public sealed class KaCompoundAccess(
    operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>
) : KaLifetimeOwner {
    private val backingOperationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> = operationPartiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingOperationPartiallyAppliedSymbol.token

    /**
     * The function that compute the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> get() = withValidityAssertion { backingOperationPartiallyAppliedSymbol }

    /**
     * A compound access that read, compute, and write the computed value back. Note that calls to `<op>Assign` is not represented by this.
     */
    public class CompoundAssign(
        operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
        kind: Kind,
        operand: KtExpression,
    ) : KaCompoundAccess(operationPartiallyAppliedSymbol) {
        public val kind: Kind by validityAsserted(kind)
        public val operand: KtExpression by validityAsserted(operand)

        public enum class Kind {
            PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN
        }

    }

    /**
     * A compound access that read, increment or decrement, and write the computed value back.
     */
    public class IncOrDecOperation(
        operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
        kind: Kind,
        precedence: Precedence,
    ) : KaCompoundAccess(operationPartiallyAppliedSymbol) {
        public val kind: Kind by validityAsserted(kind)
        public val precedence: Precedence by validityAsserted(precedence)

        public enum class Kind {
            INC, DEC
        }

        public enum class Precedence {
            PREFIX, POSTFIX
        }
    }
}