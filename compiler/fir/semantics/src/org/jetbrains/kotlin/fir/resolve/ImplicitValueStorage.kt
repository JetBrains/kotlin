/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.declarations.FirTowerDataElement
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirThisOwnerSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.util.PersistentSetMultimap
import org.jetbrains.kotlin.name.Name

class ImplicitValueStorage private constructor(
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

    /**
     * Contains implicit receivers, context receivers and context parameters.
     * Only used by DFA to apply smart casts using [ImplicitValueStorage.replaceImplicitValueType].
     * Therefore, it's just a helper and [FirTowerDataElement] should be considered the actual source of truth.
     */
    @ImplicitValue.ImplicitValueInternals
    val implicitValues: Collection<ImplicitValue>
        get() = implicitValuesBySymbol.values

    fun addAllImplicitReceivers(receivers: List<ImplicitReceiverValue<*>>): ImplicitValueStorage {
        return receivers.fold(this) { acc, value -> acc.addImplicitReceiver(name = null, value) }
    }

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>, aliasLabel: Name? = null): ImplicitValueStorage {
        val stack = implicitReceiverStack.add(value)
        val receiversPerLabel = implicitReceiversByLabel.putIfNameIsNotNull(name, value).putIfNameIsNotNull(aliasLabel, value)
        val implicitValuesBySymbol = implicitValuesBySymbol.put(value.boundSymbol, value)

        return ImplicitValueStorage(
            stack,
            receiversPerLabel,
            implicitValuesBySymbol,
        )
    }


    fun addAllContexts(
        contextReceivers: List<ContextReceiverValue>,
        contextParameters: List<ImplicitContextParameterValue>,
    ): ImplicitValueStorage {
        if (contextReceivers.isEmpty() && contextParameters.isEmpty()) {
            return this
        }

        // Not adding context receivers to implicitValuesBySymbol is a bug that leads to smart-casts not working.
        // However, we're leaving it broken because context receivers are getting removed anyway.
        return ImplicitValueStorage(
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

    /**
     * Applies smart-casted type to an [ImplicitValue] identified by its [symbol].
     *
     * Only used by DFA, and in some sense breaks persistence contracts of the data structure.
     * Therefore, it's a very fragile API and maybe should be rewritten somehow.
     */
    @ImplicitValue.ImplicitValueInternals
    fun replaceImplicitValueType(symbol: FirBasedSymbol<*>, type: ConeKotlinType) {
        val implicitValue = implicitValuesBySymbol[symbol as FirThisOwnerSymbol<*>] ?: return
        implicitValue.updateTypeFromSmartcast(type)
    }

    fun createSnapshot(keepMutable: Boolean): ImplicitValueStorage {
        return ImplicitValueStorage(
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
