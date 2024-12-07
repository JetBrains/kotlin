/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.name.Name

object ConvertibleIntegerOperators {
    val binaryOperatorsNames: Set<Name> = listOf(
        "plus", "minus", "times", "div", "rem",
        "and", "or", "xor",
        "shl", "shr", "ushr"
    ).toNameSet()

    // Constant conversion for those unary operators works only for signed integers
    val unaryOperatorNames: Set<Name> = listOf(
        "inv", "unaryPlus", "unaryMinus"
    ).toNameSet()

    val binaryOperatorsWithSignedArgument: Set<Name> = listOf(
        "shl", "shr", "ushr",
    ).toNameSet()

    private fun List<String>.toNameSet(): Set<Name> = mapTo(mutableSetOf()) { Name.identifier(it) }
}
