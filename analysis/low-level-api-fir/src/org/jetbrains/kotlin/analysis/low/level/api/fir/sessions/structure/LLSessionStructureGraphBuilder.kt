/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCacheStorage
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSourcesSession

internal object LLSessionStructureGraphBuilder {
    /**
     * Builds an [LLSessionStructureGraph] from all sessions in the given [storage].
     *
     * @param analysisRoots The list of sessions which currently have an associated cached analysis session. They are the root sessions from
     *  which resolution is started.
     */
    fun buildGraph(storage: LLFirSessionCacheStorage, analysisRoots: List<LLFirSession>): LLSessionStructureGraph {
        val sourceSessions = storage.sourceCache.values
        val librarySessions = storage.binaryCache.values

        val sessions = sourceSessions + librarySessions

        val nodesBySession = sessions
            .mapIndexed { index, session ->
                LLSessionStructureGraphNode(
                    index,
                    session,
                    LLSessionStatisticsCalculator.calculateSessionStatistics(session),
                )
            }
            .associateBy { it.session }

        assignDependencies(nodesBySession)

        val graph = LLSessionStructureGraph(nodesBySession)

        val analysisRootNodes = analysisRoots.mapNotNull { nodesBySession[it] }
        assignDistancesFromAnalysisRoots(analysisRootNodes)

        return graph
    }

    private fun assignDependencies(nodesBySession: Map<LLFirSession, LLSessionStructureGraphNode>) {
        nodesBySession.values.forEach { node ->
            val session = node.session
            if (session !is LLFirSourcesSession) return@forEach

            // The graph should only contain already cached sessions. Furthermore, we might be outside a read action here, so we shouldn't
            // compute lazily calculated dependencies.
            if (LLFirSourcesSession::dependencies.isLazyInitialized(session)) {
                node.dependencies = session.dependencies.mapNotNull(nodesBySession::get)
            }
        }
    }

    private fun assignDistancesFromAnalysisRoots(analysisRootNodes: List<LLSessionStructureGraphNode>) {
        val queue = ArrayDeque<LLSessionStructureGraphNode>()

        analysisRootNodes.forEach { rootNode ->
            rootNode.analysisRootDistance = 0
            queue.add(rootNode)
        }

        while (queue.isNotEmpty()) {
            val currentNode = queue.removeFirst()
            val currentDistance = currentNode.analysisRootDistance ?: 0
            val newDistance = currentDistance + 1

            currentNode.dependencies.forEach { dependency ->
                // Only update if we haven't visited this node yet or if we found a shorter path. This also breaks cycles.
                val currentDependencyDistance = dependency.analysisRootDistance
                if (currentDependencyDistance == null || currentDependencyDistance > newDistance) {
                    dependency.analysisRootDistance = newDistance
                    queue.add(dependency)
                }
            }
        }
    }
}
