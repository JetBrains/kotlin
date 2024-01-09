/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType

internal fun BirMemberAccessExpression<*>.checkArgumentSlotAccess(kind: String, index: Int, total: Int) {
    if (index >= total) {
        throw AssertionError(
            "No such $kind argument slot in ${this::class.java.simpleName}: $index (total=$total)" +
                    (symbol.signature?.let { ".\nSymbol: $it" } ?: "")
        )
    }
}
