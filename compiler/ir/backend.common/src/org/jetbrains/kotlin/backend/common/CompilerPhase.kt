/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

interface CompilerPhase {
    val description: String
    val prerequisite: Set<CompilerPhase>
}

class CompilerPhases<Phase>(phaseArray: Array<Phase>, config: CompilerConfiguration)
        where Phase : CompilerPhase, Phase : Enum<Phase> {

    val phases = phaseArray.associate { it.name to it }

    val enabled = computeEnabled(config)
    val verbose = phaseSetFromConfiguration(config, CommonConfigurationKeys.VERBOSE_PHASES)

    val toDumpStateBefore: Set<Phase>
    val toDumpStateAfter: Set<Phase>

    init {
        with(CommonConfigurationKeys) {
            val beforeSet = phaseSetFromConfiguration(config, PHASES_TO_DUMP_STATE_BEFORE)
            val afterSet = phaseSetFromConfiguration(config, PHASES_TO_DUMP_STATE_AFTER)
            val bothSet = phaseSetFromConfiguration(config, PHASES_TO_DUMP_STATE)
            toDumpStateBefore = beforeSet + bothSet
            toDumpStateAfter = afterSet + bothSet
        }
    }

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

            println(String.format("%1$-30s%2$-30s%3$-10s", "$key:", phase.description, "$enabled $verbose"))
        }
    }

    private fun computeEnabled(config: CompilerConfiguration) =
        with(CommonConfigurationKeys) {
            val disabledPhases = phaseSetFromConfiguration(config, DISABLED_PHASES)
            phases.values.toSet() - disabledPhases
        }

    private fun phaseSetFromConfiguration(config: CompilerConfiguration, key: CompilerConfigurationKey<Set<String>>): Set<Phase> {
        val phaseNames = config.get(key) ?: emptySet()
        if ("ALL" in phaseNames) return phases.values.toSet()
        return phaseNames.map { phases[it]!! }.toSet()
    }
}

interface PhaseRunner<Context : CommonBackendContext, Data> {
    fun reportBefore(context: Context, data: Data, phase: CompilerPhase, depth: Int)
    fun runBody(context: Context, phase: CompilerPhase, body: () -> Unit)
    fun reportAfter(context: Context, data: Data, phase: CompilerPhase, depth: Int)
}

/* We assume that `element` is being modified by each phase, retaining its identity in the process. */
class CompilerPhaseManager<Context : CommonBackendContext, Data, Phase>(
    val context: Context,
    val phases: CompilerPhases<Phase>,
    val data: Data,
    private val phaseRunner: PhaseRunner<Context, Data>,
    val parent: CompilerPhaseManager<Context, *, Phase>? = null
) where Phase : CompilerPhase, Phase : Enum<Phase> {
    val depth: Int = parent?.depth?.inc() ?: 0

    private val previousPhases = mutableSetOf<Phase>()

    fun <NewData> createChild(
        newData: NewData,
        newPhaseRunner: PhaseRunner<Context, NewData>
    ) = CompilerPhaseManager(
        context, phases, newData, newPhaseRunner, parent = this
    )

    fun createChild() = createChild(data, phaseRunner)

    private fun checkPrerequisite(phase: CompilerPhase): Boolean =
        previousPhases.contains(phase) || parent?.checkPrerequisite(phase) == true

    fun phase(phase: Phase, body: () -> Unit) {

        if (phase !in phases.enabled) return

        phase.prerequisite.forEach {
            if (!checkPrerequisite(it))
                throw Error("$phase requires $it")
        }

        previousPhases.add(phase)

        phaseRunner.reportBefore(context, data, phase, depth)
        phaseRunner.runBody(context, phase, body)
        phaseRunner.reportAfter(context, data, phase, depth)
    }
}