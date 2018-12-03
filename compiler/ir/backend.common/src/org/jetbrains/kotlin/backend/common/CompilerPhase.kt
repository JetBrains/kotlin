/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.dump
import kotlin.system.measureTimeMillis

interface CompilerPhase<in Context : BackendContext, Data> {
    val name: String
    val description: String
    val prerequisite: Set<CompilerPhase<*, *>>
        get() = emptySet()

    fun invoke(context: Context, input: Data): Data
}

private typealias AnyPhase = CompilerPhase<*, *>

class CompilerPhases(private val phaseList: List<AnyPhase>, config: CompilerConfiguration) {

    val phases = phaseList.associate { it.name to it }

    val enabled = computeEnabled(config)
    val verbose = phaseSetFromConfiguration(config, CommonConfigurationKeys.VERBOSE_PHASES)

    val toDumpStateBefore: Set<AnyPhase>
    val toDumpStateAfter: Set<AnyPhase>

    val toValidateStateBefore: Set<AnyPhase>
    val toValidateStateAfter: Set<AnyPhase>

    init {
        with(CommonConfigurationKeys) {
            val beforeDumpSet = phaseSetFromConfiguration(config, PHASES_TO_DUMP_STATE_BEFORE)
            val afterDumpSet = phaseSetFromConfiguration(config, PHASES_TO_DUMP_STATE_AFTER)
            val bothDumpSet = phaseSetFromConfiguration(config, PHASES_TO_DUMP_STATE)
            toDumpStateBefore = beforeDumpSet + bothDumpSet
            toDumpStateAfter = afterDumpSet + bothDumpSet
            val beforeValidateSet = phaseSetFromConfiguration(config, PHASES_TO_VALIDATE_BEFORE)
            val afterValidateSet = phaseSetFromConfiguration(config, PHASES_TO_VALIDATE_AFTER)
            val bothValidateSet = phaseSetFromConfiguration(config, PHASES_TO_VALIDATE)
            toValidateStateBefore = beforeValidateSet + bothValidateSet
            toValidateStateAfter = afterValidateSet + bothValidateSet
        }
    }

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -Xlist-phases to see the list of phases.")
        }
        return name
    }

    fun list() {
        phaseList.forEach { phase ->
            val enabled = if (phase in enabled) "(Enabled)" else ""
            val verbose = if (phase in verbose) "(Verbose)" else ""

            println(String.format("%1$-30s %2$-50s %3$-10s", "${phase.name}:", phase.description, "$enabled $verbose"))
        }
    }

    private fun computeEnabled(config: CompilerConfiguration) =
        with(CommonConfigurationKeys) {
            val disabledPhases = phaseSetFromConfiguration(config, DISABLED_PHASES)
            phases.values.toSet() - disabledPhases
        }

    private fun phaseSetFromConfiguration(config: CompilerConfiguration, key: CompilerConfigurationKey<Set<String>>): Set<AnyPhase> {
        val phaseNames = config.get(key) ?: emptySet()
        if ("ALL" in phaseNames) return phases.values.toSet()
        return phaseNames.map { phases[it]!! }.toSet()
    }
}


interface PhaseRunner<Context : BackendContext, Data> {
    fun runBefore(phase: CompilerPhase<Context, Data>, depth: Int, context: Context, data: Data)
    fun runBody(phase: CompilerPhase<Context, Data>, context: Context, source: Data): Data
    fun runAfter(phase: CompilerPhase<Context, Data>, depth: Int, context: Context, data: Data)
}

abstract class DefaultIrPhaseRunner<Context : CommonBackendContext, Data : IrElement>(private val validator: (data: Data, context: Context) -> Unit = { _, _ -> }) :
    PhaseRunner<Context, Data> {

    enum class BeforeOrAfter { BEFORE, AFTER }

    abstract val startPhaseMarker: CompilerPhase<Context, Data>
    abstract val endPhaseMarker: CompilerPhase<Context, Data>

    private var inVerbosePhase = false

    final override fun runBefore(phase: CompilerPhase<Context, Data>, depth: Int, context: Context, data: Data) {
        checkAndRun(phase, phases(context).toDumpStateBefore) { dumpElement(data, phase, context, BeforeOrAfter.BEFORE) }
        checkAndRun(phase, phases(context).toValidateStateBefore) { validator(data, context) }
    }

    final override fun runBody(phase: CompilerPhase<Context, Data>, context: Context, source: Data): Data {
        val runner = when {
            phase === startPhaseMarker -> ::justRun
            phase === endPhaseMarker -> ::justRun
            needProfiling(context) -> ::runAndProfile
            else -> ::justRun
        }

        inVerbosePhase = phase in phases(context).verbose

        val result = runner(phase, context, source)

        inVerbosePhase = false

        return result
    }

    final override fun runAfter(phase: CompilerPhase<Context, Data>, depth: Int, context: Context, data: Data) {
        checkAndRun(phase, phases(context).toDumpStateAfter) { dumpElement(data, phase, context, BeforeOrAfter.AFTER) }
        checkAndRun(phase, phases(context).toValidateStateAfter) { validator(data, context) }
    }

    open fun separator(title: String) = println("\n\n--- $title ----------------------\n")

    protected abstract fun phases(context: Context): CompilerPhases
    protected abstract fun elementName(input: Data): String
    protected abstract fun configuration(context: Context): CompilerConfiguration

    private fun needProfiling(context: Context) = configuration(context).getBoolean(CommonConfigurationKeys.PROFILE_PHASES)

    private fun shouldBeDumped(context: Context, input: Data) =
        elementName(input) !in configuration(context).get(CommonConfigurationKeys.EXCLUDED_ELEMENTS_FROM_DUMPING, emptySet())

    private fun checkAndRun(phase: CompilerPhase<Context, Data>, set: Set<AnyPhase>, block: () -> Unit) {
        if (phase in set) block()
    }

    private fun dumpElement(input: Data, phase: CompilerPhase<Context, Data>, context: Context, beforeOrAfter: BeforeOrAfter) {
        // Exclude nonsensical combinations
        if (phase === startPhaseMarker && beforeOrAfter == BeforeOrAfter.AFTER) return
        if (phase === endPhaseMarker && beforeOrAfter == BeforeOrAfter.BEFORE) return

        if (!shouldBeDumped(context, input)) return

        val title = when (phase) {
            startPhaseMarker -> "IR for ${elementName(input)} at the start of lowering process"
            endPhaseMarker -> "IR for ${elementName(input)} at the end of lowering process"
            else -> {
                val beforeOrAfterStr = beforeOrAfter.name.toLowerCase()
                "IR for ${elementName(input)} $beforeOrAfterStr ${phase.description}"
            }
        }
        separator(title)
        println(input.dump())
    }

    private fun runAndProfile(phase: CompilerPhase<Context, Data>, context: Context, source: Data): Data {
        var result: Data = source
        val msec = measureTimeMillis { result = phase.invoke(context, source) }
        println("${phase.description}: $msec msec")
        return result
    }

    private fun justRun(phase: CompilerPhase<Context, Data>, context: Context, source: Data) =
        phase.invoke(context, source)
}

class CompilerPhaseManager<Context : BackendContext, Data>(
    val context: Context,
    val phases: CompilerPhases,
    val data: Data,
    private val phaseRunner: PhaseRunner<Context, Data>,
    val parent: CompilerPhaseManager<Context, *>? = null
) {
    val depth: Int = parent?.depth?.inc() ?: 0

    private val previousPhases = mutableSetOf<CompilerPhase<Context, Data>>()

    fun <NewData> createChild(
        newData: NewData,
        newPhaseRunner: PhaseRunner<Context, NewData>
    ) = CompilerPhaseManager(
        context, phases, newData, newPhaseRunner, parent = this
    )

    fun createChild() = createChild(data, phaseRunner)

    private fun checkPrerequisite(phase: CompilerPhase<*, *>): Boolean =
        previousPhases.contains(phase) || parent?.checkPrerequisite(phase) == true

    fun phase(phase: CompilerPhase<Context, Data>, context: Context, source: Data): Data {

        if (phase !in phases.enabled) return source

        phase.prerequisite.forEach {
            if (!checkPrerequisite(it))
                throw Error("$phase requires $it")
        }

        previousPhases.add(phase)

        phaseRunner.runBefore(phase, depth, context, source)
        val result = phaseRunner.runBody(phase, context, source)
        phaseRunner.runAfter(phase, depth, context, result)
        return result
    }
}

fun <Context : BackendContext, Data> makePhase(
    lowering: (Context, Data) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<CompilerPhase<*, *>> = emptySet()
) = object : CompilerPhase<Context, Data> {
    override val name = name
    override val description = description
    override val prerequisite = prerequisite

    override fun invoke(context: Context, input: Data): Data {
        lowering(context, input)
        return input
    }

    override fun toString() = "Compiler Phase @$name"
}