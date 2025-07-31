/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.render

internal object IrConstTypeChecker : IrElementChecker<IrConst>(IrConst::class) {
    override fun check(element: IrConst, context: CheckerContext) {
        val irBuiltIns = context.irBuiltIns

        @Suppress("UNUSED_VARIABLE")
        val naturalType = when (element.kind) {
            IrConstKind.Null -> {
                if (!element.type.isNullable())
                    context.error(element, "expected a nullable type, got ${element.type.render()}")
                return
            }
            IrConstKind.Boolean -> irBuiltIns.booleanType
            IrConstKind.Char -> irBuiltIns.charType
            IrConstKind.Byte -> irBuiltIns.byteType
            IrConstKind.Short -> irBuiltIns.shortType
            IrConstKind.Int -> irBuiltIns.intType
            IrConstKind.Long -> irBuiltIns.longType
            IrConstKind.String -> irBuiltIns.stringType
            IrConstKind.Float -> irBuiltIns.floatType
            IrConstKind.Double -> irBuiltIns.doubleType
        }

        /*
        TODO: This check used to have JS inline class helpers. Rewrite it in a common way.
        var type = expression.type
        while (true) {
            val inlinedClass = type.getInlinedClass() ?: break
            if (getInlineClassUnderlyingType(inlinedClass) == type)
                break
            type = getInlineClassUnderlyingType(inlinedClass)
        }
        expression.ensureTypesEqual(type, naturalType)
        */
    }
}