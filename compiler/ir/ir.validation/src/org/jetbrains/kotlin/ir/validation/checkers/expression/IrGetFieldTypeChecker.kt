/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.ensureTypeIs

object IrGetFieldTypeChecker : IrElementChecker<IrGetField>(IrGetField::class) {
    override fun check(element: IrGetField, context: CheckerContext) {
        val fieldType = element.symbol.owner.type
        // TODO: We don't have the proper type substitution yet, so skip generics for now.
        if (fieldType is IrSimpleType &&
            fieldType.classifier is IrClassSymbol &&
            fieldType.arguments.isEmpty()
        ) {
            element.ensureTypeIs(fieldType, context)
        }
    }
}