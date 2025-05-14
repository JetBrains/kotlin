/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
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

/**
 * An immutable view of [ImplicitValue]s stored in a [FirTowerDataContext]'s [FirTowerDataElement]s that's convenient for certain kind of queries.
 *
 * [FirTowerDataElement]s are the source of truth, therefore, the data must always be updated together.
 */
class ImplicitValueStorage private constructor(
    private val implicitReceiverStack: PersistentList<ImplicitReceiver<*>>,
    private val implicitReceiversByLabel: PersistentSetMultimap<Name, ImplicitReceiver<*>>,
    private val implicitValuesBySymbol: PersistentMap<FirThisOwnerSymbol<*>, ImplicitValue<*>>
) {
    constructor() : this(
        persistentListOf(),
        PersistentSetMultimap(),
        persistentMapOf(),
    )

    val implicitReceivers: List<ImplicitReceiver<*>>
        get() = implicitReceiverStack

    /**
     * Contains implicit receivers, context receivers and context parameters.
     * Among other things, used by DFA to apply smart casts using [ImplicitValueStorage.replaceImplicitValueType].
     */
    val implicitValues: Collection<ImplicitValue<*>>
        get() = implicitValuesBySymbol.values

    fun addAllImplicitReceivers(receivers: List<ImplicitReceiver<*>>): ImplicitValueStorage {
        return receivers.fold(this) { acc, value -> acc.addImplicitReceiver(name = null, value) }
    }

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiver<*>, aliasLabel: Name? = null): ImplicitValueStorage {
        val stack = implicitReceiverStack.add(value)
        val receiversPerLabel = implicitReceiversByLabel.putIfNameIsNotNull(name, value).putIfNameIsNotNull(aliasLabel, value)
        val implicitValuesBySymbol =
            if (value is ImplicitValue<*>) implicitValuesBySymbol.put(value.boundSymbol, value) else implicitValuesBySymbol

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

        return ImplicitValueStorage(
            implicitReceiverStack,
            contextReceivers.fold(implicitReceiversByLabel) { acc, value -> acc.putIfNameIsNotNull(value.labelName, value) },
            implicitValuesBySymbol.addAll(contextParameters).addAll(contextReceivers),
        )
    }

    private fun PersistentMap<FirThisOwnerSymbol<*>, ImplicitValue<*>>.addAll(
        contextParameters: List<ImplicitValue<*>>,
    ): PersistentMap<FirThisOwnerSymbol<*>, ImplicitValue<*>> {
        return contextParameters.fold(this) { acc, value ->
            acc.put(value.boundSymbol, value)
        }
    }

    private fun PersistentSetMultimap<Name, ImplicitReceiver<*>>.putIfNameIsNotNull(name: Name?, value: ImplicitReceiver<*>) =
        if (name != null)
            put(name, value)
        else
            this

    operator fun get(name: String?): Set<ImplicitReceiverValue<*>> {
        if (name == null) return implicitReceiverStack.filterIsInstance<ImplicitReceiverValue<*>>().lastOrNull()?.let(::setOf).orEmpty()
        return implicitReceiversByLabel[Name.identifier(name)].filterIsInstance<ImplicitReceiverValue<*>>().toSet()
    }

    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? {
        return implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
    }

    fun lastDispatchReceiver(lookupCondition: (ImplicitReceiverValue<*>) -> Boolean): ImplicitDispatchReceiverValue? {
        return implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull(lookupCondition)
    }

    fun receiversAsReversed(): List<ImplicitReceiver<*>> = implicitReceiverStack.asReversed()

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

    fun createSnapshot(keepMutable: Boolean): ImplicitValueStorage = ImplicitValueStorage(
        implicitReceiverStack = implicitReceiverStack.map { it.createSnapshot(keepMutable) }.toPersistentList(),
        implicitReceiversByLabel = implicitReceiversByLabel.entries.fold(PersistentSetMultimap()) { accOuterMap, (name, receiverValues) ->
            receiverValues.fold(accOuterMap) { accMap, receiverValue ->
                accMap.put(name, receiverValue.createSnapshot(keepMutable))
            }
        },
        implicitValuesBySymbol = implicitValuesBySymbol.mapValues { (_, v) -> v.createSnapshot(keepMutable) }.toPersistentMap(),
    )
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
