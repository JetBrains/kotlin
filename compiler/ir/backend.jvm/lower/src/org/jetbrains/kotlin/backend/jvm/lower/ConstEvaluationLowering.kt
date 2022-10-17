/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrConstTransformer
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors

val constEvaluationPhase = makeIrModulePhase(
    ::ConstEvaluationLowering,
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`"
)

// TODO make context common
class ConstEvaluationLowering(val context: JvmBackendContext) : FileLoweringPass {
    val configuration = IrInterpreterConfiguration(printOnlyExceptionMessage = true)
    val interpreter = IrInterpreter(IrInterpreterEnvironment(context.irBuiltIns, configuration), emptyMap())

    override fun lower(irFile: IrFile) {
        fun onError(element: IrElement, error: IrErrorExpression) {
            context.ktDiagnosticReporter.at(element, irFile)
                .report(JvmBackendErrors.EXCEPTION_IN_CONST_VAL_INITIALIZER, error.description)
        }

        fun onWarning(element: IrElement, warning: IrErrorExpression) {
            context.ktDiagnosticReporter.at(element, irFile)
                .report(JvmBackendErrors.EXCEPTION_IN_CONST_EXPRESSION, warning.description)
        }

        val suppressErrors = context.configuration.getBoolean(JVMConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS)
        val transformer = IrConstTransformer(
            interpreter, irFile, mode = EvaluationMode.ONLY_INTRINSIC_CONST, ::onWarning, ::onError, suppressErrors
        )
        irFile.transformChildren(transformer, null)
    }
}

