/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

/**
 * A Cache that maintains
 *   1) mapping from CFG to collected property initialization info
 *   2) initialized status of member properties
 *
 * [FirMemberPropertiesChekcer] will analyze constructors, anonymous initializers, and property initializers to determine if member
 * properties are initialized. To avoid redundant control-flow analysis, that checker will collect information for member properties
 * as well as local properties. Then, when [FirControlFlowAnalyzer] wants to analyze those functions again, the cache will be used.
 *
 * But, if we cache everything, the mappings may grow too much. For regular uses (from [FirControlFlowAnalyzer]), we don't need caching.
 * We will do so only for constructors, anonymous initializers, and property initializers visited ahead by [FirMemberPropertiesChecker].
 */
class PropertyInitializationInfoCache private constructor() {
    // Mappings from CFG to collected (local and member) property initialization info
    private val propertyInitializationInfoCache: MutableMap<ControlFlowGraph, Map<CFGNode<*>, PathAwarePropertyInitializationInfo>> =
        mutableMapOf()

    // Cached initialization info for member properties (after analyzing constructors, anonymous initializers, and property initializers)
    // The absence/presence means that the member property of interest is un/initialized, respectively.
    private val memberPropertyInitializationCache: MutableSet<FirPropertySymbol> = mutableSetOf()

    fun getOrCollectPropertyInitializationInfo(
        graph: ControlFlowGraph,
        properties: Set<FirPropertySymbol>,
        caching: Boolean = false,
    ): Map<CFGNode<*>, PathAwarePropertyInitializationInfo> {
        return if (caching) {
            propertyInitializationInfoCache.computeIfAbsent(graph) {
                PropertyInitializationInfoCollector(properties).getData(graph)
            }
        } else {
            if (graph in this) {
                propertyInitializationInfoCache[graph]!!
            } else {
                PropertyInitializationInfoCollector(properties).getData(graph)
            }
        }
    }

    fun recordInitializedMemberProperty(property: FirPropertySymbol) {
        memberPropertyInitializationCache.add(property)
    }

    fun isInitializedMemberProperty(property: FirPropertySymbol): Boolean {
        return property in memberPropertyInitializationCache
    }

    /**
     * Invalidate cache for the given [graph]. Return `true` if there was a cached data.
     *
     * It is likely that the stale cache would be not used anyway, e.g., if the containing declaration is edited in IDE, FIR tree is
     * re-computed from scratch, hence a different control-flow graph. However, if we let the cache grow only, it may degrade performance.
     * It's still up to cache users (checker components or IDE) to invalidate cache for (soon-to-be-rebuilt) graph.
     */
    fun invalidateCache(graph: ControlFlowGraph): Boolean {
        return propertyInitializationInfoCache.remove(graph) != null
    }

    private operator fun contains(graph: ControlFlowGraph): Boolean {
        return graph in propertyInitializationInfoCache.keys
    }

    companion object {
        fun create(): PropertyInitializationInfoCache {
            return PropertyInitializationInfoCache()
        }
    }
}
