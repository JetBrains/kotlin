/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.calls.ContextParameterValue
import org.jetbrains.kotlin.fir.resolve.calls.ContextReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitValue
import org.jetbrains.kotlin.fir.resolve.calls.referencedMemberSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirThisOwnerSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.util.PersistentSetMultimap
import org.jetbrains.kotlin.name.Name

class ImplicitValueStack private constructor(
    private val implicitReceiverStack: PersistentList<ImplicitReceiverValue<*>>,
    private val implicitReceiversByLabel: PersistentSetMultimap<Name, ImplicitReceiverValue<*>>,
    private val implicitValuesBySymbol: PersistentMap<FirThisOwnerSymbol<*>, ImplicitValue>
) {
    constructor() : this(
        persistentListOf(),
        PersistentSetMultimap(),
        persistentMapOf(),
    )

    val implicitReceivers: List<ImplicitReceiverValue<*>>
        get() = implicitReceiverStack

    val implicitValues: Collection<ImplicitValue>
        get() = implicitValuesBySymbol.values

    fun addAllImplicitReceivers(receivers: List<ImplicitReceiverValue<*>>): ImplicitValueStack {
        return receivers.fold(this) { acc, value -> acc.addImplicitReceiver(name = null, value) }
    }

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>, aliasLabel: Name? = null): ImplicitValueStack {
        val stack = implicitReceiverStack.add(value)
        val receiversPerLabel = implicitReceiversByLabel.putIfNameIsNotNull(name, value).putIfNameIsNotNull(aliasLabel, value)
        val implicitValuesBySymbol = implicitValuesBySymbol.put(value.boundSymbol, value)

        return ImplicitValueStack(
            stack,
            receiversPerLabel,
            implicitValuesBySymbol,
        )
    }


    fun addAllContexts(
        contextReceivers: List<ContextReceiverValue>,
        contextParameters: List<ContextParameterValue>,
    ): ImplicitValueStack {
        if (contextReceivers.isEmpty() && contextParameters.isEmpty()) {
            return this
        }

        // Not adding context receivers to implicitValuesBySymbol is a bug that leads to smart-casts not working.
        // However, we're leaving it broken because context receivers are getting removed anyway.
        return ImplicitValueStack(
            implicitReceiverStack,
            contextReceivers.fold(implicitReceiversByLabel) { acc, value -> acc.putIfNameIsNotNull(value.labelName, value) },
            contextParameters.fold(implicitValuesBySymbol) { acc, value -> acc.put(value.boundSymbol, value) },
        )
    }

    private fun PersistentSetMultimap<Name, ImplicitReceiverValue<*>>.putIfNameIsNotNull(name: Name?, value: ImplicitReceiverValue<*>) =
        if (name != null)
            put(name, value)
        else
            this

    operator fun get(name: String?): Set<ImplicitReceiverValue<*>> {
        if (name == null) return implicitReceiverStack.lastOrNull()?.let(::setOf).orEmpty()
        return implicitReceiversByLabel[Name.identifier(name)]
    }

    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    fun lastDispatchReceiver(lookupCondition: (ImplicitReceiverValue<*>) -> Boolean): ImplicitDispatchReceiverValue? {
        return implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull(lookupCondition)
    }

    fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = implicitReceiverStack.asReversed()

    // This method is only used from DFA and it's in some sense breaks persistence contracts of the data structure
    // But it's ok since DFA handles everything properly yet, but still may be it should be rewritten somehow
    @OptIn(ImplicitValue.ImplicitValueInternals::class)
    fun replaceImplicitValueType(symbol: FirBasedSymbol<*>, type: ConeKotlinType) {
        val implicitValue = implicitValuesBySymbol[symbol as FirThisOwnerSymbol<*>] ?: return
        implicitValue.updateTypeFromSmartcast(type)
    }

    fun createSnapshot(keepMutable: Boolean): ImplicitValueStack {
        return ImplicitValueStack(
            implicitReceiverStack.map { it.createSnapshot(keepMutable) }.toPersistentList(),
            implicitReceiversByLabel,
            implicitValuesBySymbol,
        )
    }
}

fun Set<ImplicitReceiverValue<*>>.singleWithoutDuplicatingContextReceiversOrNull(): ImplicitReceiverValue<*>? {
    // KT-69102: we may encounter a bug with duplicated context receivers, and it wasn't obvious how to fix it
    return distinctBy { if (it.isContextReceiver) it.boundSymbol else it }.singleOrNull()
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
