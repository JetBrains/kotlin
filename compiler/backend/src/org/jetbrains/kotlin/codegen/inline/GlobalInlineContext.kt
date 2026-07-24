/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.threadLocal
import java.util.*

class GlobalInlineContext {
    // Ordered set of declarations and inline calls being generated right now.
    // No call in it should point to a declaration that's before it in the stack.
    private val inlineCallsAndDeclarations by threadLocal { LinkedList<Any? /* IrFunction | InlineFunctionSource? */>() }
    private val inlineDeclarationSet by threadLocal { mutableSetOf<IrFunction>() }

    private val typesUsedInInlineFunctions by threadLocal { LinkedList<MutableSet<String>>() }

    fun enterDeclaration(function: IrFunction) {
        assert(function !in inlineDeclarationSet) { "entered inlining cycle on ${function.render()}" }
        inlineDeclarationSet.add(function)
        inlineCallsAndDeclarations.add(function)
    }

    fun exitDeclaration() {
        inlineDeclarationSet.remove(inlineCallsAndDeclarations.removeLast())
    }

    fun enterIntoInlining(
        callee: IrFunction?,
        element: InlineFunctionSource?,
        reportInlineCallCycle: (InlineFunctionSource, IrFunction) -> Unit,
    ): Boolean {
        if (callee != null && callee in inlineDeclarationSet) {
            element?.let { reportInlineCallCycle(it, callee) }
            for ([call, callTarget] in inlineCallsAndDeclarations.dropWhile { it != callee }.zipWithNext()) {
                // Every call element should be followed by the callee's IR function.
                if (call is InlineFunctionSource && callTarget is IrFunction) {
                    reportInlineCallCycle(call, callTarget)
                }
            }
            return false
        }
        inlineCallsAndDeclarations.add(element)
        typesUsedInInlineFunctions.push(hashSetOf())
        return true
    }

    fun exitFromInlining() {
        inlineCallsAndDeclarations.removeLast()
        val pop = typesUsedInInlineFunctions.pop()
        typesUsedInInlineFunctions.peek()?.addAll(pop)
    }

    fun recordTypeFromInlineFunction(type: String) = typesUsedInInlineFunctions.peek().add(type)

    fun isTypeFromInlineFunction(type: String) = typesUsedInInlineFunctions.peek().contains(type)

    abstract class InlineFunctionSource
}
