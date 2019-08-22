/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

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

    // Unary
    EXCL("!"),
    // Type
    IS("is"),
    NOT_IS("!is"),
    AS("as"),
    SAFE_AS("as?"),
    // All non-standard operations (infix calls)
    OTHER;

    companion object {
        val ASSIGNMENTS: Set<FirOperation> = EnumSet.of(ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN)

        val BOOLEANS: Set<FirOperation> = EnumSet.of(
            EQ, NOT_EQ, IDENTITY, NOT_IDENTITY, LT, GT, LT_EQ, GT_EQ, IS, NOT_IS
        )

        val COMPARISONS: Set<FirOperation> = EnumSet.of(LT, GT, LT_EQ, GT_EQ)

        val TYPES: Set<FirOperation> = EnumSet.of(IS, NOT_IS, AS, SAFE_AS)
    }
}