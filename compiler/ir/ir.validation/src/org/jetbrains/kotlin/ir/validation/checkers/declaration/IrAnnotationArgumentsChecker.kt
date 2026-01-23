/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getAllArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.IrTypeChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrAnnotationArgumentsChecker : IrElementChecker<IrMutableAnnotationContainer>(IrMutableAnnotationContainer::class), IrTypeChecker {
    override fun check(element: IrMutableAnnotationContainer, context: CheckerContext) {
        checkAnnotations(element, context)
    }

    override fun check(type: IrType, container: IrElement, context: CheckerContext) {
        checkAnnotations(type, context)
    }

    private fun checkAnnotations(container: IrAnnotationContainer, context: CheckerContext) {
        fun IrElement.isConst(): Boolean {
            return this is IrConst || this is IrGetEnumValue || this is IrClassReference || (this is IrConstructorCall && type.isAnnotation())
        }

        container.annotations.forEach { annotation ->
            fun IrElement.checkIsConst(param: IrValueParameter) {
                if (isConst()) return
                if (this is IrErrorExpression && this.description.startsWith("Stub expression")) return
                context.error(
                    annotation,
                    "IR annotation has non constant argument for parameter ${param.name}. Argument: ${this.render()}"
                )
            }

            annotation.getAllArgumentsWithIr().forEach { (param, arg) ->
                val actualArg = arg ?: param.defaultValue?.expression
                when (actualArg) {
                    null -> context.error(
                        annotation,
                        "IR annotation has null argument for parameter ${param.name}."
                    )
                    is IrVararg -> actualArg.elements.forEach { element -> element.checkIsConst(param) }
                    else -> actualArg.checkIsConst(param)
                }
            }
        }
    }
}