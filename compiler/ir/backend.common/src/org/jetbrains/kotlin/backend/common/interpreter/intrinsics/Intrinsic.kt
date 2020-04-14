/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.intrinsics

import org.jetbrains.kotlin.backend.common.interpreter.ExecutionResult
import org.jetbrains.kotlin.backend.common.interpreter.stack.Stack
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction

interface Intrinsic {
    fun equalTo(irFunction: IrFunction): Boolean
    suspend fun evaluate(irFunction: IrFunction, stack: Stack, interpret: suspend IrElement.() -> ExecutionResult): ExecutionResult
}