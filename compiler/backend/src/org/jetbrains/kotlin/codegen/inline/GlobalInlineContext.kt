/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.utils.threadLocal
import java.util.*

class GlobalInlineContext {
    // Ordered set of declarations and inline calls being generated right now.
    // No call in it should point to a declaration that's before it in the stack.
    private val inlineCallsAndDeclarations by threadLocal { LinkedList<Any? /* CallableDescriptor | InlineFunctionSource? */>() }
    private val inlineDeclarationSet by threadLocal { mutableSetOf<CallableDescriptor>() }

    private val typesUsedInInlineFunctions by threadLocal { LinkedList<MutableSet<String>>() }

    fun enterDeclaration(descriptor: CallableDescriptor) {
        assert(descriptor.original !in inlineDeclarationSet) { "entered inlining cycle on $descriptor" }
        inlineDeclarationSet.add(descriptor.original)
        inlineCallsAndDeclarations.add(descriptor.original)
    }

    fun exitDeclaration() {
        inlineDeclarationSet.remove(inlineCallsAndDeclarations.removeLast())
    }

    fun enterIntoInlining(
        callee: CallableDescriptor?,
        element: InlineFunctionSource?,
        reportInlineCallCycle: (InlineFunctionSource, CallableDescriptor) -> Unit,
    ): Boolean {
        if (callee != null && callee.original in inlineDeclarationSet) {
            element?.let { reportInlineCallCycle(it, callee.original) }
            for ((call, callTarget) in inlineCallsAndDeclarations.dropWhile { it != callee.original }.zipWithNext()) {
                // Every call element should be followed by the callee's descriptor.
                if (call is InlineFunctionSource && callTarget is CallableDescriptor) {
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
