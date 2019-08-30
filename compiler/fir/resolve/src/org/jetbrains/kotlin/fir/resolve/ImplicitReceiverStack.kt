/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.name.Name

class ImplicitReceiverStack {
    private val stack: MutableList<ImplicitReceiverValue<*>> = mutableListOf()
    // This multi-map holds indexes of the stack ^
    private val indexesPerLabel: SetMultimap<Name, Int> = LinkedHashMultimap.create()

    fun add(name: Name, value: ImplicitReceiverValue<*>) {
        stack += value
        indexesPerLabel.put(name, stack.size - 1)
    }

    fun pop(name: Name) {
        indexesPerLabel.remove(name, stack.size - 1)
        stack.removeAt(stack.size - 1)
    }

    operator fun get(name: String?): ImplicitReceiverValue<*>? {
        if (name == null) return stack.lastOrNull()
        return indexesPerLabel[Name.identifier(name)].lastOrNull()?.let { stack[it] }
    }

    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = stack.asReversed()
}