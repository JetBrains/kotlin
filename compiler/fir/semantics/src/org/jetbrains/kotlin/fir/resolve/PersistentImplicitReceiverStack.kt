/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.resolve.calls.ContextReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.util.PersistentSetMultimap
import org.jetbrains.kotlin.name.Name

class PersistentImplicitReceiverStack private constructor(
    private val stack: PersistentList<ImplicitReceiverValue<*>>,
    // This multi-map holds indexes of the stack ^
    private val receiversPerLabel: PersistentSetMultimap<Name, ImplicitReceiverValue<*>>,
    private val indexesPerSymbol: PersistentMap<FirBasedSymbol<*>, Int>,
) : ImplicitReceiverStack(), Iterable<ImplicitReceiverValue<*>> {
    val size: Int get() = stack.size

    constructor() : this(
        persistentListOf(),
        PersistentSetMultimap(),
        persistentMapOf(),
    )

    fun addAll(receivers: List<ImplicitReceiverValue<*>>): PersistentImplicitReceiverStack {
        return receivers.fold(this) { acc, value -> acc.add(name = null, value) }
    }

    fun addAllContextReceivers(receivers: List<ContextReceiverValue<*>>): PersistentImplicitReceiverStack {
        return receivers.fold(this) { acc, value -> acc.addContextReceiver(value) }
    }

    fun add(name: Name?, value: ImplicitReceiverValue<*>, aliasLabel: Name? = null): PersistentImplicitReceiverStack {
        val stack = stack.add(value)
        val index = stack.size - 1
        val receiversPerLabel = receiversPerLabel.putIfNameIsNotNull(name, value).putIfNameIsNotNull(aliasLabel, value)
        val indexesPerSymbol = indexesPerSymbol.put(value.boundSymbol, index)

        return PersistentImplicitReceiverStack(
            stack,
            receiversPerLabel,
            indexesPerSymbol,
        )
    }

    private fun PersistentSetMultimap<Name, ImplicitReceiverValue<*>>.putIfNameIsNotNull(name: Name?, value: ImplicitReceiverValue<*>) =
        if (name != null)
            put(name, value)
        else
            this

    fun addContextReceiver(value: ContextReceiverValue<*>): PersistentImplicitReceiverStack {
        val labelName = value.labelName ?: return this

        val receiversPerLabel = receiversPerLabel.put(labelName, value)
        return PersistentImplicitReceiverStack(
            stack,
            receiversPerLabel,
            indexesPerSymbol,
        )
    }

    override operator fun get(name: String?): Set<ImplicitReceiverValue<*>> {
        if (name == null) return stack.lastOrNull()?.let(::setOf).orEmpty()
        return receiversPerLabel[Name.identifier(name)]
    }

    override fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    override fun lastDispatchReceiver(lookupCondition: (ImplicitReceiverValue<*>) -> Boolean): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull(lookupCondition)
    }

    override fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = stack.asReversed()

    override operator fun iterator(): Iterator<ImplicitReceiverValue<*>> {
        return stack.iterator()
    }

    // This method is only used from DFA and it's in some sense breaks persistence contracts of the data structure
    // But it's ok since DFA handles everything properly yet, but still may be it should be rewritten somehow
    @OptIn(ImplicitReceiverValue.ImplicitReceiverInternals::class)
    fun replaceReceiverType(symbol: FirBasedSymbol<*>, type: ConeKotlinType) {
        val index = indexesPerSymbol[symbol] ?: return
        stack[index].updateTypeFromSmartcast(type)
    }

    fun createSnapshot(keepMutable: Boolean): PersistentImplicitReceiverStack {
        return PersistentImplicitReceiverStack(
            stack.map { it.createSnapshot(keepMutable) }.toPersistentList(),
            receiversPerLabel,
            indexesPerSymbol,
        )
    }
}
