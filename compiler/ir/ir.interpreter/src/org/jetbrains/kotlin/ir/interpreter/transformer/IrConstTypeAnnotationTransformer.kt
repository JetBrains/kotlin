/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitor

internal class IrConstTypeAnnotationTransformer(
    interpreter: IrInterpreter,
    irFile: IrFile,
    mode: EvaluationMode,
    checker: IrInterpreterChecker,
    evaluatedConstTracker: EvaluatedConstTracker?,
    inlineConstTracker: InlineConstTracker?,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    suppressExceptions: Boolean,
) : IrConstAnnotationTransformer(
    interpreter, irFile, mode, checker, evaluatedConstTracker, inlineConstTracker, onWarning, onError, suppressExceptions
) {
    fun visitElement(element: IrElement, data: Data) {
        element.accept(
            object : IrTypeVisitor<Unit, Data>() {
                override fun visitElement(element: IrElement, data: Data) {
                    element.acceptChildren(this, data)
                }

                override fun visitType(container: IrElement, type: IrType, data: Data) {
                    transformAnnotations(type)
                }
            },
            data,
        )
    }
}
