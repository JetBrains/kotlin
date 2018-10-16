/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement

interface CompilerPhase {
    val description: String
    val prerequisite: Set<CompilerPhase>
}

class CompilerPhases<Phase>(phaseArray: Array<Phase>, config: CompilerConfiguration)
        where Phase : CompilerPhase, Phase : Enum<Phase> {

    val phases = phaseArray.associate { it.name to it }

    val enabled = computeEnabled(config)
    val verbose = config.get(CommonConfigurationKeys.VERBOSE_PHASES)?.map { phases[it]!! }?.toSet() ?: emptySet()

    private val dumpPhases = computeDumpPhases(config)
    val toDumpStateBefore = dumpPhases.component1()
    val toDumpStateAfter = dumpPhases.component2()

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -Xlist-phases to see the list of phases.")
        }
        return name
    }

    fun list() {
        phases.forEach { key, phase ->
            val enabled = if (phase in enabled) "(Enabled)" else ""
            val verbose = if (phase in verbose) "(Verbose)" else ""

            println(String.format("%1$-30s%2$-30s%3$-10s", "${key}:", phase.description, "$enabled $verbose"))
        }
    }

    private fun computeEnabled(config: CompilerConfiguration) =
        with(config) {
            with(CommonConfigurationKeys) {
                val disabledPhases = get(DISABLED_PHASES)?.map { phases[it]!! } ?: emptyList()
                val enabledPhases = get(ENABLED_PHASES)?.map { phases[it]!! } ?: emptyList()
                phases.values.toSet() - disabledPhases + enabledPhases
            }
        }

    private fun computeDumpPhases(config: CompilerConfiguration): Pair<Set<Phase>, Set<Phase>> {
        val beforeSet = config.get(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_BEFORE)?.map { phases[it]!! }?.toSet() ?: emptySet()
        val afterSet = config.get(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_AFTER)?.map { phases[it]!! }?.toSet() ?: emptySet()
        val bothSet = config.get(CommonConfigurationKeys.PHASES_TO_DUMP_STATE)?.map { phases[it]!! }?.toSet() ?: emptySet()

        return Pair(beforeSet + bothSet, afterSet + bothSet)
    }
}

interface PhaseRunner<Context : CommonBackendContext> {
    fun reportBefore(context: Context, element: IrElement, phase: CompilerPhase, depth: Int)
    fun runBody(context: Context, body: () -> Unit)
    fun reportAfter(context: Context, element: IrElement, phase: CompilerPhase, depth: Int)
}

/* We assume that `element` is being modified by each phase, retaining its identity in the process. */
class CompilerPhaseManager<Context : CommonBackendContext, Phase>(
    val context: Context,
    val phases: CompilerPhases<Phase>,
    val element: IrElement,
    val phaseRunner: PhaseRunner<Context>,
    val parent: CompilerPhaseManager<Context, Phase>? = null
) where Phase : CompilerPhase, Phase : Enum<Phase> {
    val depth: Int = parent?.depth?.inc() ?: 0

    val previousPhases = mutableSetOf<Phase>()

    fun createChild(
        newElement: IrElement = element,
        newPhaseRunner: PhaseRunner<Context> = phaseRunner
    ) = CompilerPhaseManager(
        context, phases, newElement, newPhaseRunner, parent = this
    )

    private fun checkPrerequisite(phase: CompilerPhase): Boolean =
        previousPhases.contains(phase) || parent?.checkPrerequisite(phase) == true

    fun phase(phase: Phase, body: () -> Unit) {

        if (phase !in phases.enabled) return

        phase.prerequisite.forEach {
            if (!checkPrerequisite(it))
                throw Error("$phase requires $it")
        }

        previousPhases.add(phase)

        phaseRunner.reportBefore(context, element, phase, depth)
        phaseRunner.runBody(context, body)
        phaseRunner.reportAfter(context, element, phase, depth)
    }
}