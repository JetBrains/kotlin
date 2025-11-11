/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils.inferencelogs

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger.*
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import kotlin.collections.get
import kotlin.collections.iterator

class FixationOnlyInferenceLogsDumper() : FirInferenceLogsDumper() {
    override fun renderDump(sessionsToLoggers: Map<FirSession, FirInferenceLogger>): String =
        sessionsToLoggers.entries.joinToString("\n") { (_, logger) ->
            logger.topLevelElements.renderList().orEmpty().joinToString("\n")
        }

    override fun monospace(text: String): String = text

    override fun formatCode(code: Any): String = monospace(code.toString())

    private fun LoggingElement.render(): String? {
        return when (this) {
            is BlockElement -> render()
            is FixationLogRecordElement -> render()
            else -> null
        }
    }

    private fun List<LoggingElement>.renderList(): List<String>? =
        mapNotNull { it.render() }.takeIf { it.isNotEmpty() }

    private fun BlockElement.render(): String? {
        val contents = items.renderList()?.joinToString("\n") ?: return null

        return when (owner) {
            BlockOwner.Unknown -> "*** WARNING: block for unidentified constraints system ***\n$contents"
            else -> contents
        }
    }

    private fun FixationLogRecordElement.render(): String = record.render()

    private fun InferenceLogger.FixationLogRecord.render(): String {
        val lines = mutableListOf<String?>()

        if (chosen != null) {
            lines += "CHOSEN for fixation: $chosen --- " + map[chosen]?.render()

            if (fixedTo != null) {
                lines += "    FIXED TO: $fixedTo"
            }
        }

        for ((variable, info) in map) {
            if (variable === chosen) continue
            lines += "$variable --- " + info.render()
        }

        lines += "********************************"
        return lines.joinToString("\n")
    }

    private fun InferenceLogger.FixationLogVariableInfo<*>.render(): String {
        val lines = listOf(renderReadiness(4)) + constraints.mapIndexed { index, constraint ->
            val operator = when (constraint.kind) {
                ConstraintKind.LOWER -> ">:"
                ConstraintKind.UPPER -> "<:"
                ConstraintKind.EQUALITY -> "="
            }
            val suffix = when {
                index >= constraintsBeforeFixationCount -> " (inferred during fixation)"
                else -> ""
            }
            "     $operator ${constraint.type}$suffix"
        }
        return lines.joinToString("\n")
    }
}
