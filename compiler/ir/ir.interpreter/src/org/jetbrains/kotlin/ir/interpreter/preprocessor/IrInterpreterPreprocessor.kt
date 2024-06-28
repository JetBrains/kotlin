/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.preprocessor

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

interface IrInterpreterPreprocessor: IrElementTransformer<IrInterpreterPreprocessorData> {
    fun preprocess(file: IrFile, data: IrInterpreterPreprocessorData): IrFile {
        return file.transform(this, data)
    }
}

class IrInterpreterPreprocessorData(
    val mode: EvaluationMode,
    val irBuiltIns: IrBuiltIns
)