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
 * The type of compound operation applied to a variable or an array element using the array access convention.
 *
 * The left-hand operand (the variable or array element expression) is represented in the call variants, [KaVariableAccessCall] and
 * [KaCompoundArrayAccessCall], which contain the [KaCompoundOperation].
 */
public sealed interface KaCompoundOperation : KaLifetimeOwner {
    /**
     * The function that computes the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
}

/**
 * A [compound assignment](https://kotlinlang.org/docs/operator-overloading.html#augmented-assignments) that reads, computes, and writes the
 * computed value. Calls to `<op>Assign` are not represented by [KaCompoundAssignOperation].
 */
@KaExperimentalApi
public interface KaCompoundAssignOperation : KaCompoundOperation {
    /**
     * The kind of assignment operation (`+=`, `-=`, and so on).
     */
    public val kind: Kind

    /**
     * The *right-hand* operand of the compound assignment.
     *
     * As for the *left-hand* operand (the variable or array element expression), it is represented in the call variants,
     * [KaVariableAccessCall] and [KaCompoundArrayAccessCall], which contain the [KaCompoundOperation].
     */
    public val operand: KtExpression

    @KaExperimentalApi
    public enum class Kind {
        /**
         * The `+=` assignment operation.
         */
        PLUS_ASSIGN,

        /**
         * The `-=` assignment operation.
         */
        MINUS_ASSIGN,

        /**
         * The `*=` assignment operation.
         */
        TIMES_ASSIGN,

        /**
         * The `/=` assignment operation.
         */
        DIV_ASSIGN,

        /**
         * The `%=` assignment operation.
         */
        REM_ASSIGN,
    }
}

/**
 * A [compound unary access](https://kotlinlang.org/docs/operator-overloading.html#increments-and-decrements) that reads, increments or
 * decrements, and writes the computed value.
 */
@KaExperimentalApi
public interface KaCompoundUnaryOperation : KaCompoundOperation {
    /**
     * The kind of compound unary operation (`++` or `--`).
     */
    public val kind: Kind

    /**
     * Whether the operator is syntactically applied before or after the operand.
     */
    public val precedence: Precedence

    @KaExperimentalApi
    public enum class Kind {
        /**
         * The `++` increment operation.
         */
        INC,

        /**
         * The `--` decrement operation.
         */
        DEC
    }

    @KaExperimentalApi
    public enum class Precedence {
        /**
         * The operator is a prefix of the operand (e.g. `++a`).
         */
        PREFIX,

        /**
         * The operator is a suffix of the operand (e.g. `a++`).
         */
        POSTFIX,
    }
}
