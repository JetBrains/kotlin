/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.transformer.runConstOptimizations

class ConstEvaluationLowering(
    val context: CommonBackendContext,
    private val suppressErrors: Boolean = context.configuration.getBoolean(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS),
    configuration: IrInterpreterConfiguration = IrInterpreterConfiguration(printOnlyExceptionMessage = true),
) : FileLoweringPass {
    private val interpreter = IrInterpreter(IrInterpreterEnvironment(context.irBuiltIns, configuration), emptyMap())
    private val evaluatedConstTracker = context.configuration[CommonConfigurationKeys.EVALUATED_CONST_TRACKER]
    private val inlineConstTracker = context.configuration[CommonConfigurationKeys.INLINE_CONST_TRACKER]
    private val mode = EvaluationMode.ONLY_INTRINSIC_CONST

    override fun lower(irFile: IrFile) {
        irFile.runConstOptimizations(interpreter, mode, evaluatedConstTracker, inlineConstTracker, suppressErrors)
    }
}

