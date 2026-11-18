/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import net.jqwik.api.*
import net.jqwik.api.lifecycle.AfterTry
import net.jqwik.api.statistics.Statistics
import net.jqwik.api.state.Action
import net.jqwik.api.state.ActionChain
import net.jqwik.api.state.Transformer
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.Edit
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.EditKind
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.Edits
import org.jetbrains.kotlin.buildtools.tests.fuzzing.model.ProgramPrinter

/**
 * Property-based fuzzing of the JVM incremental compilation: a random program is built, randomly edited and rebuilt,
 * and after every edit the incremental build has to be equivalent to a clean rebuild (see [org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcOracle]).
 *
 * The number of tries is set with the `kotlin.ic.fuzz.tries` Gradle property, the shrinking time limit with `kotlin.ic.fuzz.shrinkingSeconds`.
 * To replay a failure, put the seed reported by jqwik into `@Property(seed = "...")`, or paste the reproducer from the failure message into a regular test.
 * With the `kotlin.ic.fuzz.survey` Gradle property, a violation does not fail the property: every violation is only saved,
 * to collect the distinct failures of a run.
 * The `kotlin.ic.fuzz.focus` Gradle property lists the edit kinds whose weights are multiplied by [FOCUS_FACTOR],
 * to exercise the edits which need a rare program.
 */
class IcFuzzingTest {
    private val states = mutableListOf<FuzzState>()

    /** The initial edits of the current try, read when the chain creates its initial state. */
    private var currentInitialEdits: List<Edit> = emptyList()

    // the initial edits and the chain are separate parameters, so that jqwik shrinks them independently
    @Property(shrinking = ShrinkingMode.BOUNDED, afterFailure = AfterFailureMode.SAMPLE_FIRST)
    fun incrementalBuildIsEquivalentToRebuild(
        @ForAll("initialEdits") initialEdits: List<Edit>,
        @ForAll("editChains") chain: ActionChain<FuzzState>,
    ) {
        currentInitialEdits = initialEdits
        val state = try {
            chain.run()
        } catch (e: AssertionError) {
            if (!survey) throw e
            Statistics.label("violation").collect(e.message?.lineSequence()?.first()?.substringAfter(": "))
            return
        }
        Statistics.label("builds").collect(state.steps.size)
        for (step in state.steps.drop(1)) {
            Statistics.label("edit").collect(step.kind?.name ?: "REVERT")
            Statistics.label("rebuild outcome").collect(step.builds.rebuild.outcome)
            Statistics.label("IC iterations").collect(step.builds.incremental.compiledSources.size)
        }
    }

    /** Checks the generator: the initial programs are expected to compile, otherwise the fuzzing mostly checks failed builds. */
    @Property(shrinking = ShrinkingMode.BOUNDED, afterFailure = AfterFailureMode.SAMPLE_FIRST)
    fun initialProgramsCompile(@ForAll("initialEdits") initialEdits: List<Edit>) {
        val state = newState(initialEdits)
        check(state.steps.single().builds.rebuild.isSuccessful) {
            "The generated program does not compile:\n" + state.steps.single().builds.rebuild.errors.joinToString("\n") +
                    "\n\n" + ProgramPrinter.print(state.program).entries.joinToString("\n") { [name, text] -> "// $name\n$text" }
        }
    }

    @Provide
    fun initialEdits(): Arbitrary<List<Edit>> =
        edits { it.initialWeight * focusFactor(it) }.list().ofMinSize(MIN_INITIAL_EDITS).ofMaxSize(MAX_INITIAL_EDITS)

    @Provide
    fun editChains(): Arbitrary<ActionChain<FuzzState>> = EditKind.entries
        .filter { it.weight > 0 }
        .fold(ActionChain.startWith { newState(currentInitialEdits) }) { chain, kind -> chain.withAction(kind.weight * focusFactor(kind), EditAction(kind)) }
        .withAction(REVERT_WEIGHT, RevertAction)
        .withMaxTransformations(MAX_EDITS)

    private fun newState(initialEdits: List<Edit>): FuzzState = FuzzState(initialEdits).also {
        states += it
        it.buildInitial()
    }

    @AfterTry
    fun closeStates() {
        states.forEach { it.close() }
        states.clear()
    }

    private class EditAction(private val kind: EditKind) : Action.Independent<FuzzState> {
        override fun precondition(state: FuzzState) = state.failedBuildsInRow < MAX_FAILED_BUILDS_IN_ROW

        override fun transformer(): Arbitrary<Transformer<FuzzState>> = choices().map { choices ->
            val edit = Edit(kind, choices)
            Transformer.mutate<FuzzState>(edit.toString()) { it.applyEdit(edit) }
        }
    }

    private object RevertAction : Action.Independent<FuzzState> {
        override fun precondition(state: FuzzState) = state.failedBuildsInRow > 0

        override fun transformer(): Arbitrary<Transformer<FuzzState>> = Arbitraries.just(Transformer.mutate("REVERT") { it.revert() })
    }

    companion object {
        private val survey = System.getProperty("kotlin.ic.fuzz.survey").toBoolean()

        private val focus = System.getProperty("kotlin.ic.fuzz.focus").orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).toSet()

        private const val FOCUS_FACTOR = 10

        private fun focusFactor(kind: EditKind): Int = if (kind.name in focus) FOCUS_FACTOR else 1

        private const val MIN_INITIAL_EDITS = 3
        private const val MAX_INITIAL_EDITS = 16
        private const val MAX_EDITS = 8
        private const val MAX_FAILED_BUILDS_IN_ROW = 2

        /** Higher than the total weight of the edits, so that a failed build is usually reverted at once. */
        private const val REVERT_WEIGHT = 100

        private fun choices(): Arbitrary<List<Int>> = Arbitraries.integers().between(0, 999).list().ofSize(Edits.CHOICES_PER_EDIT)

        private fun edits(weight: (EditKind) -> Int): Arbitrary<Edit> =
            Arbitraries.frequency(*EditKind.entries.filter { weight(it) > 0 }.map { Tuple.of(weight(it), it) }.toTypedArray())
                .flatMap { kind -> choices().map { Edit(kind, it) } }
    }
}
