/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.calls.ContextReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.referencedMemberSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirThisOwnerSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.util.PersistentSetMultimap
import org.jetbrains.kotlin.name.Name

class ImplicitReceiverStack private constructor(
    private val stack: PersistentList<ImplicitReceiverValue<*>>,
    // This multi-map holds indexes of the stack ^
    private val receiversPerLabel: PersistentSetMultimap<Name, ImplicitReceiverValue<*>>,
    private val indexesPerSymbol: PersistentMap<FirThisOwnerSymbol<*>, Int>,
) : Iterable<ImplicitReceiverValue<*>> {
    val size: Int get() = stack.size

    constructor() : this(
        persistentListOf(),
        PersistentSetMultimap(),
        persistentMapOf(),
    )

    fun addAll(receivers: List<ImplicitReceiverValue<*>>): ImplicitReceiverStack {
        return receivers.fold(this) { acc, value -> acc.add(name = null, value) }
    }

    fun addAllContextReceivers(receivers: List<ContextReceiverValue>): ImplicitReceiverStack {
        return receivers.fold(this) { acc, value -> acc.addContextReceiver(value) }
    }

    fun add(name: Name?, value: ImplicitReceiverValue<*>, aliasLabel: Name? = null): ImplicitReceiverStack {
        val stack = stack.add(value)
        val index = stack.size - 1
        val receiversPerLabel = receiversPerLabel.putIfNameIsNotNull(name, value).putIfNameIsNotNull(aliasLabel, value)
        val indexesPerSymbol = indexesPerSymbol.put(value.boundSymbol, index)

        return ImplicitReceiverStack(
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

    fun addContextReceiver(value: ContextReceiverValue): ImplicitReceiverStack {
        val labelName = value.labelName ?: return this

        val receiversPerLabel = receiversPerLabel.put(labelName, value)
        return ImplicitReceiverStack(
            stack,
            receiversPerLabel,
            indexesPerSymbol,
        )
    }

    operator fun get(name: String?): Set<ImplicitReceiverValue<*>> {
        if (name == null) return stack.lastOrNull()?.let(::setOf).orEmpty()
        return receiversPerLabel[Name.identifier(name)]
    }

    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    fun lastDispatchReceiver(lookupCondition: (ImplicitReceiverValue<*>) -> Boolean): ImplicitDispatchReceiverValue? {
        return stack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull(lookupCondition)
    }

    fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = stack.asReversed()

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

    fun createSnapshot(keepMutable: Boolean): ImplicitReceiverStack {
        return ImplicitReceiverStack(
            stack.map { it.createSnapshot(keepMutable) }.toPersistentList(),
            receiversPerLabel,
            indexesPerSymbol,
        )
    }
}

fun Set<ImplicitReceiverValue<*>>.singleWithoutDuplicatingContextReceiversOrNull(): ImplicitReceiverValue<*>? {
    return when {
        // KT-69102: we may encounter a bug with duplicated context receivers, and it wasn't
        // obvious how to fix it
        distinctBy { if (it.isContextReceiver) it.implicitScope else it }.count() == 1 -> this.firstOrNull()
        else -> null
    }
}

fun Set<ImplicitReceiverValue<*>>.ambiguityDiagnosticFor(labelName: String?): ConeSimpleDiagnostic {
    // This condition helps choose between an error diagnostic and a warning one to better
    // replicate the K1 behavior and avoid breaking changes.
    val areAlmostAllAnonymousFunctions = count {
        it.referencedMemberSymbol is FirAnonymousFunctionSymbol
    } >= size - 1

    val diagnostic = when {
        areAlmostAllAnonymousFunctions -> ConeSimpleDiagnostic("Clashing this@$labelName", DiagnosticKind.LabelNameClash)
        else -> ConeSimpleDiagnostic("Ambiguous this@$labelName", DiagnosticKind.AmbiguousLabel)
    }

    return diagnostic
}
