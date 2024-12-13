/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.ensureTypeIs
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType

internal object IrGetFieldTypeChecker : IrGetFieldChecker {
    override fun check(
        expression: IrGetField,
        context: CheckerContext,
    ) {
        val fieldType = expression.symbol.owner.type
        // TODO: We don't have the proper type substitution yet, so skip generics for now.
        if (fieldType is IrSimpleType &&
            fieldType.classifier is IrClassSymbol &&
            fieldType.arguments.isEmpty()
        ) {
            expression.ensureTypeIs(fieldType, context)
        }
    }
}