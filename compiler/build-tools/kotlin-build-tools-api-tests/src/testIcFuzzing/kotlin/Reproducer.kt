/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcFuzzHarness
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcOracle
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.SourcesChangesMode
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

object Reproducer {
    private const val FAILURES_DIRECTORY_PROPERTY = "kotlin.ic.fuzz.failuresDir"

    /**
     * Renders a failure report: the violated properties, the history of the builds, and a test replaying them with [icFuzzingReproducer].
     * When the violation also happens when the last change is applied on top of a clean build of the previous sources,
     * the test replays only these two builds.
     * The report is also saved into the directory set by the [FAILURES_DIRECTORY_PROPERTY] system property.
     */
    fun report(steps: List<StepRecord>, violations: List<String>): String {
        val minimized = minimize(steps)
        val report = buildString {
            appendLine("The incremental build is not equivalent to a rebuild at step ${steps.size}: ${steps.last().description.lines().first()}")
            violations.forEach { appendLine(); appendLine(it.trimEnd()) }
            appendLine()
            appendLine("Steps:")
            steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.description}")
                appendLine("   incremental: ${step.builds.incremental.outcome}, compiled ${step.builds.incremental.compiledSources}")
                appendLine("   rebuild:     ${step.builds.rebuild.outcome}")
            }
            appendLine()
            if (minimized != null) {
                appendLine("Reproducer of the last change on top of a clean build of the previous sources (paste it into the testIcFuzzingReproducers suite):")
            } else {
                appendLine("Reproducer (paste it into the testIcFuzzingReproducers suite):")
            }
            appendLine(test(minimized ?: steps))
        }
        save(report)
        return report
    }

    private fun minimize(steps: List<StepRecord>): List<StepRecord>? {
        if (steps.size <= 2) return null
        val last = steps.last()
        val workingDirectory = Files.createTempDirectory("ic-fuzz-minimize")
        try {
            IcFuzzHarness(workingDirectory).use { harness ->
                val description = "build the sources before step ${steps.size}"
                val initialBuilds = harness.build(last.previousSources, emptySet())
                if (IcOracle.check(initialBuilds).isNotEmpty()) return null
                val builds = harness.build(last.sources, last.touchedFiles)
                if (IcOracle.check(builds).isEmpty()) return null
                return listOf(
                    StepRecord(description, emptyMap(), last.previousSources, emptySet(), initialBuilds),
                    StepRecord(last.description, last.previousSources, last.sources, last.touchedFiles, builds),
                )
            }
        } finally {
            @OptIn(ExperimentalPathApi::class)
            workingDirectory.deleteRecursively()
        }
    }

    private fun save(report: String) {
        val directory = System.getProperty(FAILURES_DIRECTORY_PROPERTY)?.let { Paths.get(it) } ?: return
        directory.createDirectories()
        directory.resolve("failure-${report.hashCode().toUInt()}.txt").writeText(report)
        directory.resolve("latest.txt").writeText(report)
    }

    private fun test(steps: List<StepRecord>): String = buildString {
        appendLine("@Test")
        appendLine("fun testIcFuzzingReproducer() = icFuzzingReproducer {")
        for (step in steps) {
            step.description.lines().forEach { appendLine("    // $it") }
            val changed = step.sources.filter { [name, text] -> step.previousSources[name] != text }
            val removed = step.previousSources.keys - step.sources.keys
            appendLine("    step(")
            appendLine("        ${quote(step.description.lines().first())},")
            if (changed.isNotEmpty()) {
                appendLine("        changed = mapOf(")
                for (name in changed.keys.sorted()) {
                    appendLine("            ${quote(name)} to \"\"\"")
                    changed.getValue(name).trimEnd().lines().forEach { appendLine("                |${it.replace("$", "\${'$'}")}") }
                    appendLine("            \"\"\".trimMargin(),")
                }
                appendLine("        ),")
            }
            if (removed.isNotEmpty()) appendLine("        removed = setOf(${removed.sorted().joinToString { quote(it) }}),")
            if (step.touchedFiles.isNotEmpty()) appendLine("        touched = setOf(${step.touchedFiles.sorted().joinToString { quote(it) }}),")
            appendLine("    )")
        }
        append("}")
    }

    private fun quote(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
}

class ReproducerScope internal constructor(private val harness: IcFuzzHarness) {
    private var stepNumber = 0

    /** Changes the sources and checks that the incremental build is equivalent to a rebuild. */
    fun step(
        description: String,
        changed: Map<String, String> = emptyMap(),
        removed: Set<String> = emptySet(),
        touched: Set<String> = emptySet(),
    ) {
        stepNumber++
        val builds = harness.build(harness.sources - removed + changed, touched)
        val violations = IcOracle.check(builds)
        if (violations.isNotEmpty()) {
            throw AssertionError("Step $stepNumber ($description):\n\n" + violations.joinToString("\n\n"))
        }
    }
}

/** Replays a sequence of source changes reported by [Reproducer]. */
fun icFuzzingReproducer(sourcesChangesMode: SourcesChangesMode = SourcesChangesMode.KNOWN, block: ReproducerScope.() -> Unit) {
    val workingDirectory = Files.createTempDirectory("ic-fuzz-reproducer")
    try {
        IcFuzzHarness(workingDirectory, sourcesChangesMode).use { ReproducerScope(it).block() }
    } finally {
        @OptIn(ExperimentalPathApi::class)
        workingDirectory.deleteRecursively()
    }
}
