/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

enum class FirOperation(val operator: String = "???") {
    // Binary
    RANGE(".."),
    EQ("=="),
    NOT_EQ("!="),
    IDENTITY("==="),
    NOT_IDENTITY("!=="),
    LT("<"),
    GT(">"),
    LT_EQ("<="),
    GT_EQ(">="),
    AND("&&"),
    OR("||"),
    IN("in"),
    NOT_IN("!in"),

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
        val ASSIGNMENTS = setOf(ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN)
    }
}