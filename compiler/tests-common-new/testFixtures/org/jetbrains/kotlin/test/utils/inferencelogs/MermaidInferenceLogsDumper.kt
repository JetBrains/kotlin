/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils.inferencelogs

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger.*
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger.Companion.sanitizeFqNames
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

class MermaidInferenceLogsDumper(
    /**
     * Breaks too wide diagrams resulting from the regular topological sorting
     * by assigning all further elements the next rank.
     * IJ's Mermaid plugin can't zoom, so the space is precious :(
     */
    private val maxElementsPerRank: Int = 3,
    /**
     * Turns `kotlin/Comparable<kotlin/Long>` into `Comparable<Long>` to make
     * the diagrams narrower.
     */
    private val renderPackageQualifiers: Boolean = false,
    /**
     * Render the supertype above the subtype instead of `A <: B` and `A == B`.
     * Makes the diagrams narrower, but the unconventional notation may be confusing.
     */
    private val renderConstraintsVertically: Boolean = false,
    /**
     * Sometimes edges may pass behind nodes, but visually it's a bit hard to tell
     * because they look quite similar to edges that originate from these nodes.
     * This property adds colors to edges and nodes' borders so that nearby elements
     * stay visually distinct.
     */
    private val markEdgesWithColors: Boolean = true,
) : FirInferenceLogsDumper() {
    override fun renderDump(sessionsToLoggers: Map<FirSession, FirInferenceLogger>): String {
        val header = listOf(
            "flowchart TD",
            withIndent { "${indent}classDef nowrapClass text-align:center,white-space:nowrap;" },
            withIndent { "${indent}classDef callStyle fill:#f2debb,stroke:#333,stroke-width:4px;" },
            withIndent { "${indent}classDef candidateStyle fill:#f2e5ce,stroke:#333,stroke-width:4px;" },
            withIndent { "${indent}classDef stageStyle fill:#c8f0f7,stroke:#333,stroke-width:4px;" },
            withIndent { "${indent}classDef readinessStyle fill:#fff4d6,stroke:#777,stroke-width:1px;" },
        ).joinToString("\n")

        val contents = withIndent {
            sessionsToLoggers.entries.mapNotNull { (session, logger) ->
                val title = node("session", formatCode(session))
                val structure = buildAdditionalPrintingStructure(logger.topLevelElements)
                val constraintsRemapping = logger.topLevelElements.recordConstraintsRemapping()
                val rendered = withConstraintsRemapping(constraintsRemapping) {
                    val forwardEdges = logger.topLevelElements.calculateForwardEdges()

                    withCandidate(forwardEdges) {
                        structure.renderNotNullWithIndex { render() }?.join("\n\n")
                    }
                }
                listOfNotNull(title, rendered).join("\n\n")
            }.join("\n\n")
        }

        val edgesColors = when {
            markEdgesWithColors -> withIndent { generateEdgesColorsFor(contents?.rendered ?: "") }
            else -> null
        }
        return listOfNotNull(header, contents?.rendered, edgesColors).joinToString("\n\n")
    }

    override fun monospace(text: String): String = "<tt>$text</tt>"

    override fun formatCode(code: Any): String = code.toString()
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace("*", "\\*")
        .replace("*\n", "<br>")
        .let(::monospace)

    private fun formatCodeBlock(code: Any): String = formatCode(code)
        .let { "<pre style=\"display: inline-block;vertical-align: middle;text-align: left;margin: 0;\">$it</pre>" }

    private fun formatConstraint(constraint: String): String = constraint
        .let {
            if (renderPackageQualifiers) return@let it
            it.replace("""\b(?:\w+/)*""".toRegex(), "")
        }
        .let {
            if (!renderConstraintsVertically) return@let it
            it.replace("""(.*) <: (.*)""".toRegex(), "$2\n▽\n$1").replace(" == ", "\n‖\n")
        }
        .let(::formatCode)

    private data class RenderingResult(
        val rendered: String,
        val firstNodeId: String,
        val lastNodes: List<LastNodeConnectionInfo>,
    )

    private fun RenderingResult.mapLasts(transform: (LastNodeConnectionInfo) -> LastNodeConnectionInfo): RenderingResult =
        copy(lastNodes = lastNodes.map(transform))

    private data class LastNodeConnectionInfo(
        val id: String,
        // Some subgraphs with cross-connections must have their
        // own connections equal to their topological height.
        // Length 1 is considered normal, that is `-->`.
        val outgoingConnectionLength: Int,
        val outgoingConnectionStyle: ConnectionStyle = ConnectionStyle.Normal,
    )

    private enum class ConnectionStyle(val beginning: String, val middle: String, val end: String) {
        Normal("--", "-", ">"),
        Invisible("~~", "~", "~")
    }

    private var nextNodeIndex = 0

    private fun node(
        idPrefix: String,
        title: String,
        outgoingConnectionStyle: ConnectionStyle = ConnectionStyle.Normal,
        extraClasses: List<String> = emptyList(),
    ): RenderingResult {
        val id = idPrefix + nextNodeIndex++

        return RenderingResult(
            rendered = "$indent$id[\"$title\"]\n${indent}class $id nowrapClass;" +
                    extraClasses.joinToString("") { "\n${indent}class $id $it;" },
            firstNodeId = id,
            lastNodes = listOf(LastNodeConnectionInfo(id, outgoingConnectionLength = 1, outgoingConnectionStyle)),
        )
    }

    private fun LoggingElement.render(): RenderingResult? {
        return when (this) {
            is BlockElement -> render()
            is InitialConstraintElement -> render()
            is VariableConstraintElement -> render()
            is ErrorElement -> render()
            is NewVariableElement -> render()
            is FixationLogRecordElement -> render()
        }
    }

    /**
     * A wrapper over [ConstraintElement.origins] that removes [VariableConstraintElement] that
     * are direct copies of the corresponding [InitialConstraintElement]s.
     */
    private fun ConstraintElement.getOrigins(): List<ConstraintElement> =
        origins.map { printingOptions.constraintsRemaping[it] ?: it }

    private val ConstraintElement.isDuplicate: Boolean
        get() = this in printingOptions.constraintsRemaping

    private inline fun <T : LoggingElement, R> List<T>.renderListWithoutJoining(transformRender: (T, RenderingResult) -> R): List<R>? =
        renderNotNullWithIndex { _ ->
            this.render()?.let { rendered -> transformRender(this, rendered) }
        }

    private fun List<LoggingElement>.renderList(delimiter: String = "\n\n"): RenderingResult? =
        renderListWithoutJoining { _, rendered -> rendered }?.join(delimiter)

    private inline fun List<RenderingResult>.join(
        delimiter: String = "\n\n",
        connect: (RenderingResult, RenderingResult) -> String,
    ): RenderingResult? {
        if (isEmpty()) return null

        val renderedWithConnectors = mutableListOf(first().rendered)

        for (it in 1 until size) {
            val previous = this[it - 1]
            val next = this[it]

            renderedWithConnectors.add(connect(previous, next))
            renderedWithConnectors.add(next.rendered)
        }

        return RenderingResult(
            rendered = renderedWithConnectors.joinToString(delimiter),
            firstNodeId = first().firstNodeId,
            lastNodes = last().lastNodes,
        )
    }

    private fun connectLastToFirst(origin: LastNodeConnectionInfo, destinationFirstNodeId: String): String {
        val connection = origin.outgoingConnectionStyle.run {
            beginning + middle.repeat(origin.outgoingConnectionLength - 1) + end
        }
        return "$indent${origin.id} $connection $destinationFirstNodeId"
    }

    private fun connectLastsToFirst(origin: RenderingResult, destination: RenderingResult): String =
        origin.lastNodes.joinToString("\n") { node ->
            connectLastToFirst(node, destination.firstNodeId)
        }

    private fun List<RenderingResult>.join(delimiter: String = "\n\n"): RenderingResult? =
        join(delimiter, ::connectLastsToFirst)

    private fun List<BlockElement>.recordConstraintsRemapping(): Map<ConstraintElement, ConstraintElement> {
        val constraintToTransitiveOrigins = mutableMapOf<ConstraintElement, Set<ConstraintElement>>()
        val allConstraints = flatMap { it.items }.filterIsInstance<ConstraintElement>()

        for (constraint in allConstraints) {
            require(constraint.origins.isNotEmpty() || constraint is InitialConstraintElement) {
                "$constraint has no originating constraints, but is not an InitialConstraintElement. Some `withOrigins()` calls are missing."
            }
            constraintToTransitiveOrigins[constraint] = constraint.origins.fold(setOf()) { acc, element ->
                when {
                    element is InitialConstraintElement -> acc + element
                    else -> acc + constraintToTransitiveOrigins[element].orEmpty()
                }
            }
        }

        val knownConstraintsCache = mutableMapOf<Pair<String, Set<ConstraintElement>>, ConstraintElement>()
        val constraintsRemapping = mutableMapOf<ConstraintElement, ConstraintElement>()

        for (constraint in allConstraints) {
            val transitiveOrigins = constraintToTransitiveOrigins[constraint]?.takeIf { it.isNotEmpty() } ?: continue

            if (constraint is VariableConstraintElement && constraint.origins.singleOrNull() is InitialConstraintElement) {
                constraintsRemapping[constraint] = constraint.origins.single()
                continue
            }

            val key = constraint.renderRelation() to transitiveOrigins
            val cached = knownConstraintsCache[key]

            if (cached != null) {
                constraintsRemapping[constraint] = cached
            } else {
                knownConstraintsCache[key] = constraint
            }
        }

        return constraintsRemapping
    }

    private fun List<BlockElement>.calculateForwardEdges(): Map<ConstraintElement, Set<ConstraintElement>> {
        val allConstraints = flatMap { it.items }.filterIsInstance<ConstraintElement>()

        return buildMap<_, MutableSet<ConstraintElement>> {
            for (it in allConstraints) {
                for (origin in it.getOrigins()) {
                    getOrPut(origin) { mutableSetOf() }.add(it)
                }
            }
        }
    }

    private fun FixationLogRecordElement.render(): RenderingResult {
        val variable = record.chosen ?: "<no chosen>"
        val readiness = record.map[record.chosen]?.renderReadiness() ?: "<no readiness>"

        return node(
            idPrefix = "variableReadiness",
            title = "Choose " + formatCode(variable) + " with " + formatCodeBlock(readiness),
            extraClasses = listOf("readinessStyle"),
        )
    }

    private fun ConstraintElement.toKnownNodeResult(): RenderingResult = printingOptions.judgmentNodeCache[this]
        ?: error("No node for ${renderRelation()} has been registered")

    private fun PrintingNode.render(): RenderingResult? = when (this) {
        is CallNode -> render()
        is CandidateNode -> render()
        is PassthroughNode -> render()
    }

    private fun CallNode.render(): RenderingResult? {
        val title = node(
            idPrefix = "call",
            title = "Call ${index + 1}<br><br>" + formatCodeBlock(call.render),
            extraClasses = listOf("callStyle"),
        )

        val contents = candidates.renderNotNullWithIndex { _ -> render() }?.join("\n\n") ?: return null
        return listOf(title, contents).join("\n\n")
    }

    private fun CandidateNode.render(): RenderingResult? {
        @OptIn(SymbolInternals::class)
        val signature = FirRenderer.forReadability().renderElementAsString(owner.candidate.symbol.fir)
        val formattedSymbol = formatCode(owner.candidate.symbol)
        val title = node(
            idPrefix = "candidate",
            title = "Candidate ${index + 1}: $formattedSymbol" + "<br><br>" + formatCodeBlock(signature),
            extraClasses = listOf("candidateStyle"),
        )

        val contents = blocks.renderList("\n\n") ?: return null
        return listOf(title, contents).join("\n\n")
    }

    private fun PassthroughNode.render(): RenderingResult? = element.render()

    private fun BlockElement.render(): RenderingResult? {
        val mainTitle = node("stage", name, outgoingConnectionStyle = ConnectionStyle.Invisible)
        val titleStyle = "\n${indent}class ${mainTitle.firstNodeId} stageStyle;"
        val titledGraphs = items.partitionIntoTitledGraphs(mainTitle).takeIf { it.isNotEmpty() } ?: return null

        val first = titledGraphs.first()
        val rest = titledGraphs.drop(1)

        val firstRendered = first.renderConstraintsOfStage(titleStyle)
        val restRendered = rest.map { it.renderConstraintsOfStage() ?: it.title }
            .takeIf { it.isNotEmpty() }?.join("\n\n")

        if (restRendered == null) {
            return firstRendered
        }

        val mainTitleGraph = firstRendered ?: RenderingResult(
            rendered = first.title.rendered + titleStyle,
            firstNodeId = first.title.firstNodeId,
            lastNodes = first.title.lastNodes,
        )
        return listOfNotNull(mainTitleGraph, restRendered).join("\n\n")
    }

    private class TitledGraph(
        val title: RenderingResult,
        val constraints: MutableList<ConstraintElement> = mutableListOf(),
    )

    private fun List<BlockItemElement>.partitionIntoTitledGraphs(first: RenderingResult): List<TitledGraph> {
        val graphs = mutableListOf(TitledGraph(first))
        var index = 0

        for (it in this) {
            if (it !is ConstraintElement) {
                val rendered = it.render()?.also { index++ }
                    ?.mapLasts { it.copy(outgoingConnectionStyle = ConnectionStyle.Invisible) }
                    ?: continue
                graphs.add(TitledGraph(rendered))
            } else {
                graphs.last().constraints.add(it)
            }
        }

        return graphs
    }

    private fun TitledGraph.renderConstraintsOfStage(titleStyle: String = ""): RenderingResult? {
        val ownConstraints = constraints.calculateOwnConstraints()

        fun ConstraintElement.hasOriginFromThisStage(): Boolean =
            getOrigins().any { it in ownConstraints }

        fun ConstraintElement.hasDestinationFromThisStage(): Boolean =
            printingOptions.forwardEdges[this].orEmpty().any { !it.isDuplicate && it in ownConstraints }

        val ranks = calculateRanks(constraints)
        val maxRank = ranks.values.maxOrNull() ?: 0
        val tailNodes = mutableListOf<RenderingResult>()

        val rendered = constraints.renderListWithoutJoining { element, rendered ->
            val rank = ranks[element] ?: error("Missing rank")

            val connections = element.getOrigins().map {
                val originRank = ranks[it] ?: (rank - 1)
                val origin = it.toKnownNodeResult().lastNodes.first().copy(outgoingConnectionLength = rank - originRank)
                connectLastToFirst(origin, rendered.firstNodeId)
            }
            if (!element.hasDestinationFromThisStage()) {
                tailNodes += rendered.mapLasts { it.copy(outgoingConnectionLength = maxRank - rank + 1) }
            }
            val titleConnection = when {
                !element.hasOriginFromThisStage() -> connectLastToFirst(
                    title.lastNodes.first().copy(outgoingConnectionLength = rank + 1),
                    rendered.firstNodeId,
                )
                else -> null
            }

            listOfNotNull(rendered.rendered, titleConnection, *connections.toTypedArray()).joinToString("\n")
        }

        val joined = rendered?.joinToString("\n") ?: return null

        return RenderingResult(
            rendered = title.rendered + titleStyle + "\n" + joined,
            firstNodeId = title.firstNodeId,
            lastNodes = tailNodes.flatMap { it.lastNodes }.map { it.copy(outgoingConnectionStyle = ConnectionStyle.Invisible) },
        )
    }

    private fun calculateRanks(constraints: List<ConstraintElement>): MutableMap<ConstraintElement, Int> {
        val ranks = mutableMapOf<ConstraintElement, Int>()
        val rankToCount = mutableListOf<Int>()

        fun getCountForRank(rank: Int) = rankToCount.getOrNull(rank)
            ?: 0.also(rankToCount::add)

        for (element in constraints) {
            if (element.isDuplicate) continue
            var rank = 1 + (element.getOrigins().maxOfOrNull { ranks[it] ?: -1 } ?: -1)

            while (getCountForRank(rank) >= maxElementsPerRank) {
                rank++
            }

            rankToCount[rank] = getCountForRank(rank) + 1
            ranks[element] = rank
        }

        return ranks
    }

    private fun List<BlockItemElement>.calculateOwnConstraints(): Set<ConstraintElement> =
        filterIsInstance<ConstraintElement>().mapTo(mutableSetOf()) { it }

    private fun ConstraintElement.renderRelation() = when (this) {
        is InitialConstraintElement -> constraint
        is VariableConstraintElement -> constraint
    }

    private fun InitialConstraintElement.render(): RenderingResult? {
        if (isDuplicate) return null
        val position = sanitizeFqNames(position)
        return node("constraint", formatConstraint(constraint) + "<br><br><i><div style=\"display: inline-block;vertical-align: middle;\">from</div> ${formatCodeBlock(position)}</i>")
            .also { printingOptions.judgmentNodeCache.putIfAbsent(this, it) }
    }

    private fun VariableConstraintElement.render(): RenderingResult? {
        if (isDuplicate) return null
        return node("constraint", formatConstraint(constraint))
            .also { printingOptions.judgmentNodeCache.putIfAbsent(this, it) }
    }

    private fun ErrorElement.render(): RenderingResult =
        node("error", "<b>${renderErrorTitle(error)}<b>")

    private fun NewVariableElement.render(): RenderingResult =
        node("newVariable", renderVariableTitle(variable))

    private fun generateEdgesColorsFor(text: String): String {
        val edgesPattern = ConnectionStyle.entries
            .joinToString("|") { it.beginning + it.middle + "*" + it.end }
            .let { "(\\w+) (?:$it) \\w+" }
            .toRegex()

        val nextColorGenerator = generateHexColors(lightness = 0.3).iterator()
        val colorPerNode = mutableMapOf<String, String>()
        val edges = edgesPattern.findAll(text).toList()

        val lineStyles = edges.mapIndexedNotNull { index, edge ->
            val lhs = edge.groupValues[1].takeIf { it.startsWith("constraint") } ?: return@mapIndexedNotNull null
            val color = colorPerNode.getOrPut(lhs) { nextColorGenerator.next() }
            "${indent}linkStyle $index stroke:#$color"
        }

        val nodeStyles = colorPerNode.entries.map { (id, color) ->
            "${indent}style $id stroke:#$color;"
        }

        return (lineStyles + nodeStyles).joinToString("\n")
    }

    private var printingOptions = PrintingOptions()
    private val indent get() = printingOptions.indent

    private data class PrintingOptions(
        val indent: String = "",
        val constraintsRemaping: Map<ConstraintElement, ConstraintElement> = emptyMap(),
        val judgmentNodeCache: MutableMap<ConstraintElement, RenderingResult> = mutableMapOf(),
        val forwardEdges: Map<ConstraintElement, Set<ConstraintElement>> = emptyMap(),
    )

    private inline fun <T> withIndent(block: () -> T): T {
        val oldOptions = printingOptions
        return try {
            printingOptions = oldOptions.copy(indent = oldOptions.indent + "    ")
            block()
        } finally {
            printingOptions = oldOptions
        }
    }

    private inline fun <T> withCandidate(
        forwardEdges: Map<ConstraintElement, Set<ConstraintElement>>,
        block: () -> T,
    ): T {
        val oldOptions = printingOptions
        return try {
            printingOptions = oldOptions.copy(
                judgmentNodeCache = mutableMapOf(),
                forwardEdges = forwardEdges,
            )
            block()
        } finally {
            printingOptions = oldOptions
        }
    }

    private inline fun <T> withConstraintsRemapping(
        constraintsRemapping: Map<ConstraintElement, ConstraintElement>,
        block: () -> T,
    ): T {
        val oldOptions = printingOptions
        return try {
            printingOptions = oldOptions.copy(
                constraintsRemaping = constraintsRemapping,
            )
            block()
        } finally {
            printingOptions = oldOptions
        }
    }
}
