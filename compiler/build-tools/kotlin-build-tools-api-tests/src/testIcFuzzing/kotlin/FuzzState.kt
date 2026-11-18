/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcFuzzHarness
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcOracle
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.StepBuilds
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.Edit
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.EditKind
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.Edits
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.Program
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.ProgramPrinter
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

/**
 * A build of one source state: [sources] were built after [previousSources], with [touchedFiles] reported as modified.
 * [kind] is the kind of the edit which produced the state, `null` for the initial build and for a revert.
 */
class StepRecord(
    val description: String,
    val previousSources: Map<String, String>,
    val sources: Map<String, String>,
    val touchedFiles: Set<String>,
    val builds: StepBuilds,
    val kind: EditKind? = null,
)

/**
 * The state of one fuzzing try: the current program and the history of its builds.
 * Every build is checked with [IcOracle], a violation is thrown as an [AssertionError] carrying a [Reproducer].
 */
class FuzzState(private val initialEdits: List<Edit>) : AutoCloseable {
    private val workingDirectory = Files.createTempDirectory("ic-fuzz")
    private val harness = IcFuzzHarness(workingDirectory)

    var program: Program = Edits.SEED_PROGRAM
        private set
    private var lastSuccessfullyBuilt: Program? = null

    /** The number of the last builds in a row that failed (according to the rebuild). */
    var failedBuildsInRow = 0
        private set

    val steps = mutableListOf<StepRecord>()

    /** Applies the initial edits to [Edits.SEED_PROGRAM] and builds the result. */
    fun buildInitial() {
        val descriptions = initialEdits.mapNotNull { edit ->
            Edits.apply(program, edit)?.also { program = it.program }?.description
        }
        program = Edits.completeInitialProgram(program)
        build(program, "initial build:\n" + descriptions.joinToString("\n") { "  - $it" }, emptySet())
    }

    fun applyEdit(edit: Edit) {
        val outcome = Edits.apply(program, edit) ?: return
        build(outcome.program, outcome.description, outcome.touchedFiles, edit.kind)
    }

    fun revert() {
        build(lastSuccessfullyBuilt ?: Edits.SEED_PROGRAM, "revert to the last state that compiled", emptySet())
    }

    private fun build(newProgram: Program, description: String, touchedFiles: Set<String>, kind: EditKind? = null) {
        val previousSources = harness.sources
        val sources = ProgramPrinter.print(newProgram)
        val builds = harness.build(sources, touchedFiles)
        program = newProgram
        steps += StepRecord(description, previousSources, sources, touchedFiles, builds, kind)
        if (builds.rebuild.isSuccessful) {
            lastSuccessfullyBuilt = newProgram
            failedBuildsInRow = 0
        } else {
            failedBuildsInRow++
        }
        val violations = IcOracle.check(builds)
        if (violations.isNotEmpty()) {
            throw AssertionError(Reproducer.report(steps, violations))
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun close() {
        harness.close()
        workingDirectory.deleteRecursively()
    }
}
