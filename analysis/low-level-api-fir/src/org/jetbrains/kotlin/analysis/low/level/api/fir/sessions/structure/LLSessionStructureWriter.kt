/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCacheStorage
import java.io.BufferedWriter

/**
 * [LLSessionStructureWriter] writes a GraphML graph of [LLFirSession]s which is used to visualize and analyze the structure of cached
 * sessions and their memory usage.
 *
 * ### Gephi Setup
 *
 * To view the GraphML, [Gephi](https://gephi.org) is recommended. Read on for recommended settings to make the graph useful. By default,
 * the graph will likely be unreadable.
 *
 * Appearance settings:
 *
 * - Show Node Labels in the bottom Graph window
 * - Appearance (Nodes):
 *   - Color: Partition -> analysisRootDistance (always click "Apply")
 *   - Size: Ranking -> weight, 20 - 200
 *   - Label size: Unique -> 0.05
 *
 * Layouts (applied in this order):
 *
 * - Yifan Hu:
 *   - Optimal distance: 100
 *   - Relative strength: 20.0
 *   - Initial step size: 40.0
 * - Label Adjust
 *   - Nicely spaces out nodes.
 *
 * Computing degrees:
 *
 * - In the right-hand toolbar, click on Statistics and run "Average Degree".
 * - This will calculate in and out degrees for each node. Now the data can be used to visualize the degree distribution.
 */
object LLSessionStructureWriter {
    /**
     * Writes the session structure described by [storage] and [analysisRoots] to [writer].
     */
    fun writeSessionStructure(storage: LLFirSessionCacheStorage, analysisRoots: List<LLFirSession>, writer: BufferedWriter) {
        val graph = LLSessionStructureGraphBuilder.buildGraph(storage, analysisRoots)
        writeGraph(graph, writer)
    }

    private fun writeGraph(graph: LLSessionStructureGraph, writer: BufferedWriter) {
        with(writer) {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<graphml xmlns="http://graphml.graphdrawing.org/xmlns"""")
            appendLine("""         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"""")
            appendLine("""         xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd">""")

            // Define attributes for nodes
            appendLine("""  <key id="label" for="node" attr.name="label" attr.type="string"/>""")
            appendLine("""  <key id="weight" for="node" attr.name="weight" attr.type="long"/>""")
            appendLine("""  <key id="kotlinWeight" for="node" attr.name="kotlinWeight" attr.type="long"/>""")
            appendLine("""  <key id="javaWeight" for="node" attr.name="javaWeight" attr.type="long"/>""")
            appendLine("""  <key id="analysisRootDistance" for="node" attr.name="analysisRootDistance" attr.type="int"/>""")

            appendLine("""  <graph id="SessionWeightGraph" edgedefault="directed">""")

            graph.nodesBySession.values.forEach { node ->
                if (!node.isSignificant) return@forEach

                appendLine("""    <node id="n${node.id}">""")
                appendLine("""      <data key="label">${escapeXml(node.label)}</data>""")
                appendLine("""      <data key="weight">${node.statistics.weight}</data>""")
                appendLine("""      <data key="kotlinWeight">${node.statistics.kotlinWeight}</data>""")
                appendLine("""      <data key="javaWeight">${node.statistics.javaWeight}</data>""")
                node.analysisRootDistance?.let { distance ->
                    appendLine("""      <data key="analysisRootDistance">$distance</data>""")
                }
                appendLine("""    </node>""")

                node.dependencies.forEach { dependencyNode ->
                    if (!dependencyNode.isSignificant) return@forEach

                    appendLine("""    <edge source="n${node.id}" target="n${dependencyNode.id}"/>""")
                }
            }

            appendLine("""  </graph>""")
            appendLine("""</graphml>""")
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
