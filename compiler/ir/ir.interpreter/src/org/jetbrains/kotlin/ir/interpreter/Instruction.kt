/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack

internal interface Instruction {
    val element: IrElement?
}

internal class CompoundInstruction(override val element: IrElement?) : Instruction // must unwind first
internal class SimpleInstruction(override val element: IrElement) : Instruction    // must interpret as is
internal class CustomInstruction(val evaluate: () -> Unit) : Instruction {
    override val element: IrElement? = null
}

internal fun CallStack.pushSimpleInstruction(element: IrElement) {
    pushInstruction(SimpleInstruction(element))
}

internal fun CallStack.pushCompoundInstruction(element: IrElement?) {
    pushInstruction(CompoundInstruction(element))
}
