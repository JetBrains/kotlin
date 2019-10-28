/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

interface ImplicitReceiverStack {
    fun add(name: Name?, value: ImplicitReceiverValue<*>)
    fun pop(name: Name?)

    operator fun get(name: String?): ImplicitReceiverValue<*>?

    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue?
    fun receiversAsReversed(): List<ImplicitReceiverValue<*>>
}

class ImplicitReceiverStackImpl : ImplicitReceiverStack, Iterable<ImplicitReceiverValue<*>> {
    private val stack: MutableList<ImplicitReceiverValue<*>> = mutableListOf()
    private val originalTypes: MutableList<ConeKotlinType> = mutableListOf()
    // This multi-map holds indexes of the stack ^
    private val indexesPerLabel: SetMultimap<Name, Int> = LinkedHashMultimap.create()
    private val indexesPerSymbol: MutableMap<FirBasedSymbol<*>, Int> = mutableMapOf()
    val size: Int get() = stack.size

    override fun add(name: Name?, value: ImplicitReceiverValue<*>) {
        stack += value
        originalTypes += value.type
        val index = stack.size - 1
        if (name != null) {
            indexesPerLabel.put(name, index)
        }
        indexesPerSymbol.put(value.boundSymbol, index)
    }

    override fun pop(name: Name?) {
        val index = stack.size - 1
        if (name != null) {
            indexesPerLabel.remove(name, index)
        }
        originalTypes.removeAt(index)
        val value = stack.removeAt(index)
        indexesPerSymbol.remove(value.boundSymbol)
    }

    override operator fun get(name: String?): ImplicitReceiverValue<*>? {
        if (name == null) return stack.lastOrNull()
        return indexesPerLabel[Name.identifier(name)].lastOrNull()?.let { stack[it] }
    }

    override fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    override fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = stack.asReversed()

    fun getReceiverIndex(symbol: FirBasedSymbol<*>): Int? = indexesPerSymbol[symbol]

    fun getOriginalType(index: Int): ConeKotlinType {
        return originalTypes[index]
    }

    fun replaceReceiverType(index: Int, type: ConeKotlinType) {
        assert(index >= 0 && index < stack.size)
        stack[index].replaceType(type)
    }

    override operator fun iterator(): Iterator<ImplicitReceiverValue<*>> {
        return stack.iterator()
    }
}