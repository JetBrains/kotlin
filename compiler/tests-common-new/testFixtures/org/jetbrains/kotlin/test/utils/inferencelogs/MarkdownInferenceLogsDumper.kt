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
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger.FixationLogRecord

private val ANY_LINE_ENDING_REGEX = """(\r\n|\r|\n)""".toRegex()

class MarkdownInferenceLogsDumper(private val ignoreDuplicates: Boolean = true) : FirInferenceLogsDumper() {
    override fun renderDump(sessionsToLoggers: Map<FirSession, FirInferenceLogger>): String =
        sessionsToLoggers.entries.joinToString("\n\n") { (session, logger) ->
            val structure = buildAdditionalPrintingStructure(logger.topLevelElements)
            val renderedElements = structure.renderNotNullWithIndex { render(it) }
            listOf("## `${session}`", *renderedElements.orEmpty().toTypedArray()).joinToString("\n\n")
        }

    override fun monospace(text: String): String = "`$text`"

    override fun formatCode(code: Any): String = monospace(code.toString())

    private fun makeSingleLine(text: String): String = text.replace(ANY_LINE_ENDING_REGEX, "↩")

    private val stack = mutableListOf<LoggingElement>()

    private fun LoggingElement.render(indexWithinParent: Int): String? {
        val oldStackSize = stack.size
        return try {
            stack.add(this)
            when (this) {
                is BlockElement -> render()
                is InitialConstraintElement -> render(indexWithinParent)
                is VariableConstraintElement -> render(indexWithinParent)
                is ErrorElement -> render(indexWithinParent)
                is NewVariableElement -> render(indexWithinParent)
                is FixationLogRecordElement -> render(indexWithinParent)
            }
        } finally {
            while (stack.size > oldStackSize) {
                stack.removeLast()
            }
        }
    }

    private fun List<LoggingElement>.renderList(): List<String>? =
        renderNotNullWithIndex { index -> render(index) }

    private fun FixationLogRecordElement.render(indexWithinParent: Int): String = record.render(indexWithinParent)

    private fun FixationLogRecord.render(indexWithinParent: Int): String {
        val lines = mutableListOf(
            chosen?.let {
                val number = "$indent${indexWithinParent + 1}. "
                val chosenReadiness = map[chosen]?.renderReadiness(number.length) ?: error("No readiness for chosen variable")
                number + "Choose " + formatCode(it) + " with " + formatCode(chosenReadiness)
            },
        )

        withIndent {
            var index = 0
            map.mapNotNullTo(lines) { (variable, info) ->
                if (variable == chosen) return@mapNotNullTo null
                val number = "$indent${index++ + 1}. "
                number + formatCode(variable) + " is " + formatCode(info.renderReadiness(number.length))
            }
        }

        return lines.joinToString("\n")
    }

    private fun PrintingNode.render(indexWithinParent: Int): String? = when (this) {
        is CallNode -> render()
        is CandidateNode -> render()
        is PassthroughNode -> render(indexWithinParent)
    }

    private fun CallNode.render(): String? {
        val callTitle = "$indent### Call ${index + 1}"
        val code = "$indent```\n${call.render}\n```"
        val title = "$callTitle\n\n$code"

        val contents = candidates.renderNotNullWithIndex { _ -> render() }?.joinToString("\n\n") ?: return null
        return listOf(title, contents).joinToString("\n\n")
    }

    private fun CandidateNode.render(): String? {
        @OptIn(SymbolInternals::class)
        val signature = FirRenderer.forReadability().renderElementAsString(owner.candidate.symbol.fir)
        val title = "$indent#### Candidate ${index + 1}: `${owner.candidate.symbol}` --- `${makeSingleLine(signature)}`"

        val contents = blocks.renderList()?.joinToString("\n\n") ?: return null
        return listOf(title, contents).joinToString("\n")
    }

    private fun PassthroughNode.render(indexWithinParent: Int): String? = element.render(indexWithinParent)

    private fun BlockElement.render(): String? {
        val groupsByOrigin = bringStructureToStageElements(items)
        var index = 0

        val entries = groupsByOrigin.mapNotNull { group ->
            renderStageElementGroup(group, indexWithinParent = { index }, incrementIndex = { index++ })
        }.takeIf { it.isNotEmpty() } ?: return null

        return "$indent##### $name:\n\n" + entries.joinToString("\n")
    }

    private fun bringStructureToStageElements(blockItemElements: List<BlockItemElement>): List<List<BlockItemElement>> {
        if (blockItemElements.isEmpty()) return emptyList()

        val groupsByOrigin = mutableListOf(mutableListOf<BlockItemElement>())
        val seenConstraints = mutableSetOf<String>()

        for (next in blockItemElements) {
            if (ignoreDuplicates && next is ConstraintElement && next.renderRelation() in seenConstraints) {
                continue
            }

            val originElement = groupsByOrigin.last().lastOrNull()

            if (next is ConstraintElement && originElement is ConstraintElement && originElement.origins == next.origins) {
                groupsByOrigin.last().add(next)
            } else if (groupsByOrigin.last().isNotEmpty()) {
                groupsByOrigin.add(mutableListOf(next))
            } else {
                groupsByOrigin.last().add(next)
            }

            if (next is ConstraintElement) {
                seenConstraints.add(next.renderRelation())
            }
        }

        return groupsByOrigin
    }

    private inline fun renderStageElementGroup(
        group: List<BlockItemElement>,
        indexWithinParent: () -> Int,
        incrementIndex: () -> Unit,
    ): String? {
        val first = group.firstOrNull() ?: return null

        if (first !is ConstraintElement || first.origins.isEmpty()) {
            return group.mapNotNull {
                it.render(indexWithinParent())?.also { incrementIndex() }
            }.takeIf { it.isNotEmpty() }?.joinToString("\n")
        }

        val elements = withIndent { group.renderList() } ?: return null

        if (first.origins.size == 1) {
            if (first.origins.single().renderRelation() == (first.previousEntry as? ConstraintElement)?.renderRelation()) {
                return elements.joinToString("\n")
            }

            val origin = "$indent${indexWithinParent() + 1}. From `" + first.origins.single().renderRelation() + "`"
                .also { incrementIndex() }
            return "$origin\n" + elements.joinToString("\n")
        }

        val manyCombined = first.origins.joinToString(" with ") { "`" + it.renderRelation() + "`" }
        val origin = "$indent${indexWithinParent() + 1}. Combine $manyCombined".also { incrementIndex() }
        return "$origin\n" + elements.joinToString("\n")
    }

    private fun ConstraintElement.renderRelation() = when (this) {
        is InitialConstraintElement -> constraint
        is VariableConstraintElement -> constraint
    }

    private fun InitialConstraintElement.render(indexWithinParent: Int): String {
        val position = sanitizeFqNames(position)
        return "$indent${indexWithinParent + 1}. `$constraint` _from ${makeSingleLine(position)}_"
    }

    private fun VariableConstraintElement.render(indexWithinParent: Int): String {
        val formattedSelf = "`$constraint`"
        return "$indent${indexWithinParent + 1}. $formattedSelf"
    }

    private val ConstraintElement.previousEntry: BlockItemElement?
        get() {
            val outerConstraintsOwner = stack.getOrNull(stack.size - 1) as? BlockElement ?: return null
            val currentEntryIndex = outerConstraintsOwner.items.indexOf(this)
            return outerConstraintsOwner.items.getOrNull(currentEntryIndex - 1)
        }

    private fun ErrorElement.render(indexWithinParent: Int): String =
        "$indent${indexWithinParent + 1}. __${renderErrorTitle(error)}__"

    private fun NewVariableElement.render(indexWithinParent: Int): String =
        "${indent}${indexWithinParent + 1}. " + renderVariableTitle(variable)

    private var printingOptions = PrintingOptions()
    private val indent get() = printingOptions.indent

    private data class PrintingOptions(
        val indent: String = "",
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
}
