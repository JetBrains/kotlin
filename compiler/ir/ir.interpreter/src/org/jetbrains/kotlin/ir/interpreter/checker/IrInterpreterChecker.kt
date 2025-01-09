/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrInterpreterChecker : IrElementVisitor<Boolean, IrInterpreterCheckerData>

class IrInterpreterCheckerData(
    val irFile: IrFile,
    val mode: EvaluationMode,
    val irBuiltIns: IrBuiltIns,
)
