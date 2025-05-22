/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils.constraintslogger

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.inference.FirConstraintsLogger
import org.jetbrains.kotlin.fir.resolve.inference.FirConstraintsLogger.*
import org.jetbrains.kotlin.fir.resolve.inference.FirConstraintsLogger.Companion.sanitizeFqNames
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.FixationLogRecord

class MarkdownConstraintsDumper(private val ignoreDuplicates: Boolean = true) : FirConstraintsDumper() {
    override fun renderDump(sessionsToLoggers: Map<FirSession, FirConstraintsLogger>): String =
        sessionsToLoggers.entries.joinToString("\n\n") { (session, logger) ->
            val structure = buildAdditionalPrintingStructure(logger.topLevelElements)
            val renderedElements = structure.renderNotNullWithIndex { render(it) }
            listOf("## `${session}`", *renderedElements.orEmpty().toTypedArray()).joinToString("\n\n")
        }

    override fun monospace(text: String): String = "`$text`"

    override fun formatCode(code: Any): String = monospace(code.toString())

    private fun makeSingleLine(text: String): String = text.replace("\n", "↩")

    private val stack = mutableListOf<LoggingElement>()

    private fun LoggingElement.render(indexWithinParent: Int): String? {
        val oldStackSize = stack.size
        return try {
            stack.add(this)
            when (this) {
                is StageBlockElement -> render()
                is InitialConstraintElement -> render(indexWithinParent)
                is IncorporatedConstraintElement -> render(indexWithinParent)
                is ConstraintSubstitutionElement -> render(indexWithinParent)
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

    private fun FixationLogRecordElement.render(indexWithinParent: Int): String? = record.render(indexWithinParent)

    private fun FixationLogRecord.render(indexWithinParent: Int): String? {
        val lines = mutableListOf(
            chosen?.let {
                val chosenReadiness = map[chosen]?.readiness ?: error("No readiness for chosen variable")
                val number = "$indent${indexWithinParent + 1}. "
                number + "Choose " + formatCode(it) + " with " + formatCode(chosenReadiness)
            },
        )

        withIndent {
            var index = 0
            map.mapNotNullTo(lines) { (variable, info) ->
                if (variable == chosen) return@mapNotNullTo null
                val number = "$indent${index++ + 1}. "
                number + formatCode(variable) + " is " + formatCode(info.readiness)
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

        val contents = candidates.renderNotNullWithIndex { index -> render() }?.joinToString("\n\n") ?: return null
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

    private fun StageBlockElement.render(): String? {
        val groupsByOrigin = bringStructureToStageElements(elements)
        var index = 0

        val entries = groupsByOrigin.mapNotNull { group ->
            renderStageElementGroup(group, indexWithinParent = { index }, incrementIndex = { index++ })
        }.takeIf { it.isNotEmpty() } ?: return null

        return "$indent##### $name:\n\n" + entries.joinToString("\n")
    }

    private fun bringStructureToStageElements(stageElements: List<StageElement>): List<List<StageElement>> {
        if (stageElements.isEmpty()) return emptyList()

        val groupsByOrigin = mutableListOf(mutableListOf<StageElement>())
        val seenConstraints = mutableSetOf<String>()

        for (next in stageElements) {
            if (ignoreDuplicates && next is ConstraintElement && next.renderRelation() in seenConstraints) {
                continue
            }

            val previousElement = groupsByOrigin.last().lastOrNull()

            if (next is ConstraintElement && previousElement is ConstraintElement && previousElement.previous == next.previous) {
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
        group: List<StageElement>,
        indexWithinParent: () -> Int,
        incrementIndex: () -> Unit,
    ): String? {
        val first = group.firstOrNull() ?: return null

        if (first !is ConstraintElement || first.previous.isEmpty()) {
            return group.mapNotNull {
                it.render(indexWithinParent())?.also { incrementIndex() }
            }.takeIf { it.isNotEmpty() }?.joinToString("\n")
        }

        val elements = withIndent { group.renderList() } ?: return null

        if (first.previous.size == 1) {
            if (first.previous.single().renderRelation() == (first.previousEntry as? ConstraintElement)?.renderRelation()) {
                return elements.joinToString("\n")
            }

            val origin = "$indent${indexWithinParent() + 1}. From `" + first.previous.single().renderRelation() + "`"
                .also { incrementIndex() }
            return "$origin\n" + elements.joinToString("\n")
        }

        val manyCombined = first.previous.joinToString(" with ") { "`" + it.renderRelation() + "`" }
        val origin = "$indent${indexWithinParent() + 1}. Combine $manyCombined".also { incrementIndex() }
        return "$origin\n" + elements.joinToString("\n")
    }

    private fun ConstraintElement.renderRelation() = when (this) {
        is InitialConstraintElement -> constraint
        is IncorporatedConstraintElement -> constraint
        is ConstraintSubstitutionElement -> constraint
    }

    private fun InitialConstraintElement.render(indexWithinParent: Int): String? {
        val position = sanitizeFqNames(position)
        return "$indent${indexWithinParent + 1}. `$constraint` _from ${makeSingleLine(position)}_"
    }

    private fun IncorporatedConstraintElement.render(indexWithinParent: Int): String? {
        val formattedSelf = "`$constraint`"
        return "$indent${indexWithinParent + 1}. $formattedSelf"
    }

    private val ConstraintElement.previousEntry: StageElement?
        get() {
            val outerConstraintsOwner = stack.getOrNull(stack.size - 1) as? StageBlockElement ?: return null
            val currentEntryIndex = outerConstraintsOwner.elements.indexOf(this)
            return outerConstraintsOwner.elements.getOrNull(currentEntryIndex - 1)
        }

    private fun ConstraintSubstitutionElement.render(indexWithinParent: Int): String? {
        return "$indent${indexWithinParent + 1}. `$constraint`"
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
