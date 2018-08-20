/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

object OperatorNames {
    val UNARY_PLUS = OperatorNameConventions.UNARY_PLUS
    val UNARY_MINUS = OperatorNameConventions.UNARY_MINUS

    val ADD = OperatorNameConventions.PLUS
    val SUB = OperatorNameConventions.MINUS
    val MUL = OperatorNameConventions.TIMES
    val DIV = OperatorNameConventions.DIV
    val MOD = OperatorNameConventions.MOD
    val REM = OperatorNameConventions.REM

    val AND = OperatorNameConventions.AND
    val OR = OperatorNameConventions.OR
    val XOR = Name.identifier("xor")
    val INV = Name.identifier("inv")

    val SHL = Name.identifier("shl")
    val SHR = Name.identifier("shr")
    val SHRU = Name.identifier("ushr")

    val NOT = OperatorNameConventions.NOT

    val INC = OperatorNameConventions.INC
    val DEC = OperatorNameConventions.DEC


    val BINARY = setOf(ADD, SUB, MUL, DIV, MOD, REM, AND, OR, XOR, SHL, SHR, SHRU)
    val UNARY = setOf(UNARY_PLUS, UNARY_MINUS, INV, NOT, INC, DEC)
    val ALL = BINARY + UNARY
}