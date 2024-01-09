/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.utils

import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.expressions.BirBlock
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirFunctionReference
import org.jetbrains.kotlin.bir.util.getValueArgument
import org.jetbrains.kotlin.bir.util.isVararg

// Return the underlying function for a lambda argument without bound or default parameters or varargs.
fun BirExpression.asInlinableFunctionReference(): BirFunctionReference? {
    // A lambda is represented as a block with a function declaration and a reference to it.
    // Inlinable function references are also a kind of lambda; bound receivers are represented as extension receivers.
    if (this !is BirBlock || statements.size != 2)
        return null
    val (function, reference) = statements
    if (function !is BirSimpleFunction || reference !is BirFunctionReference || function != reference.symbol)
        return null
    if (function.dispatchReceiverParameter != null)
        return null
    if ((0 until reference.valueArguments.size).any { reference.valueArguments[it] != null })
        return null
    if (function.valueParameters.any { it.isVararg || it.defaultValue != null })
        return null
    return reference
}