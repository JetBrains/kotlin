/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrConstTransformer

val constEvaluationPhase = makeIrModulePhase(
    ::ConstEvaluationLowering,
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `Foldable`"
)

class ConstEvaluationLowering(val context: CommonBackendContext) : FileLoweringPass {
    val interpreter = IrInterpreter(context.irBuiltIns)

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(IrConstTransformer(interpreter, irFile, mode = EvaluationMode.ONLY_INTRINSIC_CONST), null)
    }
}

