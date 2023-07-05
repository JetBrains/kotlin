/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

enum class FirOperation(val operator: String = "???") {
    // Binary
    EQ("=="),
    NOT_EQ("!="),
    IDENTITY("==="),
    NOT_IDENTITY("!=="),
    LT("<"),
    GT(">"),
    LT_EQ("<="),
    GT_EQ(">="),

    ASSIGN("="),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    TIMES_ASSIGN("*="),
    DIV_ASSIGN("/="),
    REM_ASSIGN("%="),

    // Type
    IS("is"),
    NOT_IS("!is"),
    AS("as"),
    SAFE_AS("as?"),
    // All non-standard operations (infix calls)
    OTHER;

    companion object {
        val ASSIGNMENTS: Set<FirOperation> = EnumSet.of(ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN)

        val TYPES: Set<FirOperation> = EnumSet.of(IS, NOT_IS, AS, SAFE_AS)
    }
}

object FirOperationNameConventions {
    val ASSIGNMENTS: Map<FirOperation, Name> = EnumMap(
        mapOf(
            FirOperation.PLUS_ASSIGN to OperatorNameConventions.PLUS_ASSIGN,
            FirOperation.MINUS_ASSIGN to OperatorNameConventions.MINUS_ASSIGN,
            FirOperation.TIMES_ASSIGN to OperatorNameConventions.TIMES_ASSIGN,
            FirOperation.DIV_ASSIGN to OperatorNameConventions.DIV_ASSIGN,
            FirOperation.REM_ASSIGN to OperatorNameConventions.REM_ASSIGN
        )
    )
    val ASSIGNMENT_NAMES = ASSIGNMENTS.map { (k, v) -> v to k }.toMap()

    val ASSIGNMENTS_TO_SIMPLE_OPERATOR: Map<FirOperation, Name> = EnumMap(
        mapOf(
            FirOperation.PLUS_ASSIGN to OperatorNameConventions.PLUS,
            FirOperation.MINUS_ASSIGN to OperatorNameConventions.MINUS,
            FirOperation.TIMES_ASSIGN to OperatorNameConventions.TIMES,
            FirOperation.DIV_ASSIGN to OperatorNameConventions.DIV,
            FirOperation.REM_ASSIGN to OperatorNameConventions.REM
        )
    )
}