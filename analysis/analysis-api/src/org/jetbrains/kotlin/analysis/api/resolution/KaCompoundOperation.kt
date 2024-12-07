/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtExpression

/**
 * The type of compound access to a variable, or to an array element using the array access convention.
 */
public sealed interface KaCompoundOperation : KaLifetimeOwner {
    /**
     * The function that computes the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
}

/**
 * A compound access that reads, computes, and writes the computed value. Note that calls to `<op>Assign` are not represented by this.
 */
@KaExperimentalApi
public interface KaCompoundAssignOperation : KaCompoundOperation {
    public val kind: Kind
    public val operand: KtExpression

    @KaExperimentalApi
    public enum class Kind {
        PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN
    }
}

/**
 * A compound access that reads, increments or decrements, and writes the computed value.
 */
@KaExperimentalApi
public interface KaCompoundUnaryOperation : KaCompoundOperation {
    public val kind: Kind
    public val precedence: Precedence

    @KaExperimentalApi
    public enum class Kind {
        INC, DEC
    }

    @KaExperimentalApi
    public enum class Precedence {
        PREFIX, POSTFIX
    }
}
