/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.boxing

import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter

class BoxingFrame(nLocals: Int, nStack: Int, private val boxingInterpreter: BoxingInterpreter) : Frame<BasicValue>(nLocals, nStack) {
    override fun merge(frame: Frame<out BasicValue>, interpreter: Interpreter<BasicValue>): Boolean {
        if (stackSize != frame.stackSize) {
            throw AnalyzerException(null, "Incompatible stack heights")
        }

        var changed = false
        for (i in 0 until locals) {
            val local = getLocal(i)
            val merged = boxingInterpreter.mergeLocalVariableValues(local, frame.getLocal(i))
            if (local != merged) {
                setLocal(i, merged)
                changed = true
            }
        }
        for (i in 0 until stackSize) {
            val onStack = getStack(i)
            val merged = boxingInterpreter.mergeStackValues(onStack, frame.getStack(i))
            if (onStack != merged) {
                setStack(i, merged)
                changed = true
            }
        }
        return changed
    }
}