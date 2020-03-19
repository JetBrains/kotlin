/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

abstract class ImplicitReceiverStack : Iterable<ImplicitReceiverValue<*>> {
    abstract operator fun get(name: String?): ImplicitReceiverValue<*>?

    abstract fun lastDispatchReceiver(): ImplicitDispatchReceiverValue?
    abstract fun receiversAsReversed(): List<ImplicitReceiverValue<*>>
}

abstract class MutableImplicitReceiverStack : ImplicitReceiverStack() {
    abstract fun add(name: Name?, value: ImplicitReceiverValue<*>)
    abstract fun pop(name: Name?)

    abstract fun snapshot(): MutableImplicitReceiverStack
}

class ImplicitReceiverStackImpl private constructor(
    private var stack: PersistentList<ImplicitReceiverValue<*>>,
    // This multi-map holds indexes of the stack ^
    private var originalTypes: PersistentList<ConeKotlinType>,
    private var indexesPerLabel: PersistentSetMultimap<Name, Int>,
    private var indexesPerSymbol: PersistentMap<FirBasedSymbol<*>, Int>
) : MutableImplicitReceiverStack() {
    val size: Int get() = stack.size

    constructor() : this(
        persistentListOf(),
        persistentListOf(),
        PersistentSetMultimap(),
        persistentMapOf()
    )

    override fun add(name: Name?, value: ImplicitReceiverValue<*>) {
        stack = stack.add(value)
        originalTypes = originalTypes.add(value.type)
        val index = stack.size - 1
        if (name != null) {
            indexesPerLabel = indexesPerLabel.put(name, index)
        }
        indexesPerSymbol = indexesPerSymbol.put(value.boundSymbol, index)
    }

    override fun pop(name: Name?) {
        val index = stack.size - 1
        if (name != null) {
            indexesPerLabel = indexesPerLabel.remove(name, index)
        }
        originalTypes = originalTypes.removeAt(index)
        val value = stack.get(index)
        stack = stack.removeAt(index)
        indexesPerSymbol = indexesPerSymbol.remove(value.boundSymbol)
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

    override fun snapshot(): ImplicitReceiverStackImpl {
        return ImplicitReceiverStackImpl(stack, originalTypes, indexesPerLabel, indexesPerSymbol)
    }
}
