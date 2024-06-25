/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtExpression

/**
 * The type of access to a variable or using the array access convention.
 */
public sealed interface KaCompoundOperation : KaLifetimeOwner {
    /**
     * The function that compute the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
}

/**
 * A compound access that read, compute, and write the computed value back. Note that calls to `<op>Assign` is not represented by this.
 */
public interface KaCompoundAssignOperation : KaCompoundOperation {
    public val kind: Kind
    public val operand: KtExpression

    public enum class Kind {
        PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN
    }
}

/**
 * A compound access that read, increment or decrement, and write the computed value back.
 */
public interface KaCompoundUnaryOperation : KaCompoundOperation {
    public val kind: Kind
    public val precedence: Precedence

    public enum class Kind {
        INC, DEC
    }

    public enum class Precedence {
        PREFIX, POSTFIX
    }
}

@Deprecated("Use `KaCompoundOperation` instead", ReplaceWith("KaCompoundOperation"))
public typealias KaCompoundAccess = KaCompoundOperation
