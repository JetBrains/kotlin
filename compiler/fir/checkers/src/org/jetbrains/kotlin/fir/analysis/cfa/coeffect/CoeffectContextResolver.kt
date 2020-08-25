/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.coeffect

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.analysis.cfa.ControlFlowInfo
import org.jetbrains.kotlin.fir.contract.contextual.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor

class CoeffectContextResolver(
    val actionsOnNodes: CoeffectActionsOnNodes,
) : ControlFlowGraphVisitor<CoeffectContextOnNodes, Collection<CoeffectContextOnNodes>>() {

    override fun visitNode(node: CFGNode<*>, data: Collection<CoeffectContextOnNodes>): CoeffectContextOnNodes {
        var dataForNode = if (data.isEmpty()) CoeffectContextOnNodes.EMPTY else data.reduce(CoeffectContextOnNodes::merge)
        val actions = actionsOnNodes[node] ?: return dataForNode

        actions.forEach {
            dataForNode += it.provider
            dataForNode += it.cleaner
        }

        return dataForNode
    }
}

class CoeffectContextOnNodes(
    map: PersistentMap<CoeffectFamily, CoeffectContext> = persistentMapOf(),
) : ControlFlowInfo<CoeffectContextOnNodes, CoeffectFamily, CoeffectContext>(map) {

    companion object {
        val EMPTY = CoeffectContextOnNodes()
    }

    override val constructor: (PersistentMap<CoeffectFamily, CoeffectContext>) -> CoeffectContextOnNodes = ::CoeffectContextOnNodes

    override fun get(key: CoeffectFamily): CoeffectContext = super.get(key) ?: key.emptyContext

    fun merge(other: CoeffectContextOnNodes): CoeffectContextOnNodes {
        var result = this
        for (family in keys.union(other.keys)) {
            val context = family.combiner.merge(this[family], other[family])
            result = result.put(family, context)
        }
        return result
    }

    fun applyProvider(provider: CoeffectContextProvider?): CoeffectContextOnNodes {
        if (provider == null) return this
        val newContext = provider.provideContext(this[provider.family])
        return put(provider.family, newContext)
    }

    fun applyCleaner(cleaner: CoeffectContextCleaner?): CoeffectContextOnNodes {
        if (cleaner == null) return this
        val newContext = cleaner.cleanupContext(this[cleaner.family])
        return put(cleaner.family, newContext)
    }

    operator fun plus(provider: CoeffectContextProvider?): CoeffectContextOnNodes = applyProvider(provider)
    operator fun plus(cleaner: CoeffectContextCleaner?): CoeffectContextOnNodes = applyCleaner(cleaner)
}