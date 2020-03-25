/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.PersistentSetMultimap
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.Name

class PersistentImplicitReceiverStack private constructor(
    private val stack: PersistentList<ImplicitReceiverValue<*>>,
    // This multi-map holds indexes of the stack ^
    private val indexesPerLabel: PersistentSetMultimap<Name, Int>,
    private val indexesPerSymbol: PersistentMap<FirBasedSymbol<*>, Int>
) : ImplicitReceiverStack(), Iterable<ImplicitReceiverValue<*>> {
    val size: Int get() = stack.size

    constructor() : this(
        persistentListOf(),
        PersistentSetMultimap(),
        persistentMapOf()
    )

    fun add(name: Name?, value: ImplicitReceiverValue<*>): PersistentImplicitReceiverStack {
        val stack = stack.add(value)
        val index = stack.size - 1
        val indexesPerLabel = name?.let { indexesPerLabel.put(it, index) } ?: indexesPerLabel
        val indexesPerSymbol = indexesPerSymbol.put(value.boundSymbol, index)
        return PersistentImplicitReceiverStack(
            stack,
            indexesPerLabel,
            indexesPerSymbol
        )
    }

    override operator fun get(name: String?): ImplicitReceiverValue<*>? {
        if (name == null) return stack.lastOrNull()
        return indexesPerLabel[Name.identifier(name)].lastOrNull()?.let { stack[it] }
    }

    override fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    override fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = stack.asReversed()

    override operator fun iterator(): Iterator<ImplicitReceiverValue<*>> {
        return stack.iterator()
    }
}