/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.types.BirType

@Deprecated("Use more elegant direct array-like syntax", ReplaceWith("valueArguments[index]"))
fun BirMemberAccessExpression<*>.getValueArgument(index: Int): BirExpression? {
    return valueArguments[index]
}

@Deprecated("Use more elegant direct array-like syntax", ReplaceWith("typeArguments[index]"))
fun BirMemberAccessExpression<*>.getTypeArgument(index: Int): BirType? {
    return typeArguments[index]
}

@Deprecated("Use more elegant direct array-like syntax", ReplaceWith("valueArguments[index] = valueArgument"))
fun BirMemberAccessExpression<*>.putValueArgument(index: Int, valueArgument: BirExpression?) {
    valueArguments[index] = valueArgument
}

/*
fun BirMemberAccessExpression<*>.putTypeArgument(index: Int, type: BirType?) {
    typeArguments[index] = type
}*/
