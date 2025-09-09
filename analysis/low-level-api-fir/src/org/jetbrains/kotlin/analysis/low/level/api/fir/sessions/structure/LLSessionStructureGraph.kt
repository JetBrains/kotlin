/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure

import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession

/**
 * A graph of [LLFirSession]s following the dependency structure of the currently cached sessions. Only sessions which exist in the cache
 * are included in the graph.
 *
 * The purpose of the graph, once written to GraphML with [LLSessionStructureWriter], is to visualize and analyze the structure of
 * cached sessions and their memory usage.
 *
 * The graph has the following features:
 *
 * - Each node in the graph represents a session with relevant statistics like its weight. The label of the node is the session module's
 *   name.
 * - Directed edges represent dependencies between sessions.
 */
internal class LLSessionStructureGraph(
    val nodesBySession: Map<LLFirSession, LLSessionStructureGraphNode>,
)

/**
 * Represents a node in the [LLSessionStructureGraph].
 *
 * Each node corresponds to an [LLFirSession] with its associated [LLSessionStatistics] and dependencies. Dependencies are represented as
 * child nodes, forming a directed graph structure.
 *
 * @property id A unique numeric ID. It is used to link the node in GraphML.
 */
internal class LLSessionStructureGraphNode(
    val id: Int,
    val session: LLFirSession,
    val statistics: LLSessionStatistics,
) {
    /**
     * The graph can be circular, so we need to assign dependencies some time after all nodes have been created.
     */
    var dependencies: List<LLSessionStructureGraphNode> = emptyList()

    /**
     * The session's distance from the nearest session that has a corresponding analysis session.
     *
     * A value of 0 means that this session has a corresponding analysis session.
     */
    var analysisRootDistance: Int? = null

    val label: String
        get() = when (val module = session.ktModule) {
            is KaSourceModule -> "[SRC] ${module.name}"
            is KaLibraryModule -> "[LIB] ${module.libraryName}"

            // We cannot get the module description for a script since it might try to find the `KtFile`, which requires a read action.
            is KaScriptModule -> "[SCRIPT]"

            else -> module.moduleDescription
        }

    /**
     * Returns true if this node should be included in the GraphML output.
     *
     * Even nodes with no weight need to be included in [LLSessionStructureGraph] to properly calculate session properties, like the
     * distance to the nearest analysis session.
     */
    val isSignificant: Boolean
        get() = statistics.weight > 0
}
