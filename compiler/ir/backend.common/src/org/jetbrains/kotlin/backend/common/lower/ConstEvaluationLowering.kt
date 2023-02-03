/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrConstTransformer

class ConstEvaluationLowering(
    val context: CommonBackendContext,
    private val suppressErrors: Boolean = false,
    private val onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    private val onError: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
) : FileLoweringPass {
    val configuration = IrInterpreterConfiguration(printOnlyExceptionMessage = true)
    val interpreter = IrInterpreter(IrInterpreterEnvironment(context.irBuiltIns, configuration), emptyMap())

    override fun lower(irFile: IrFile) {
        val transformer = IrConstTransformer(
            interpreter, irFile, mode = EvaluationMode.ONLY_INTRINSIC_CONST, onWarning, onError, suppressErrors
        )
        irFile.transformChildren(transformer, null)
    }
}

