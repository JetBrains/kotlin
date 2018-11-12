/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.ir.declarations.IrFile
import kotlin.reflect.KClass

interface CompilerPhase<in Context : BackendContext, Data> {
    val name: String
    val description: String
    val prerequisite: Set<CompilerPhase<Context, *>>

    fun invoke(context: Context, source: Data): Data
}

private typealias AnyPhase = CompilerPhase<*, *>
class CompilerPhases(val phaseList: List<AnyPhase>, config: CompilerConfiguration) {

    val phases = phaseList.associate { it.name to it }

    val enabled = computeEnabled(config)
    val verbose = phaseSetFromConfiguration(config, CommonConfigurationKeys.VERBOSE_PHASES)

    val toDumpStateBefore: Set<AnyPhase>
    val toDumpStateAfter: Set<AnyPhase>

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
    fun reportBefore(phase: CompilerPhase<Context, Data>, depth: Int, context: Context, data: Data)
    fun runBody(phase: CompilerPhase<Context, Data>, context: Context, source: Data): Data
    fun reportAfter(phase: CompilerPhase<Context, Data>, depth: Int, context: Context, data: Data)
}

/* We assume that `element` is being modified by each phase, retaining its identity in the process. */
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

        phaseRunner.reportBefore(phase, depth, context, source)
        val result = phaseRunner.runBody(phase, context, source)
        phaseRunner.reportAfter(phase, depth, context, result)
        return result
    }
}

inline fun <reified Lowering : FileLoweringPass> makePhase(
    vararg args: Any?,
    description: String,
    name: String = Lowering::class.simpleName!!,
    prerequisite: Set<CompilerPhase<BackendContext, *>> = emptySet()
) =
    object : CompilerPhase<BackendContext, IrFile> {
        override val name = name
        override val description = description
        override val prerequisite = prerequisite

        override fun invoke(context: BackendContext, source: IrFile): IrFile {
            val loweringConstructorWithContext = Lowering::class.constructors.filter {
                it.parameters.size > 0 &&
                        (it.parameters[0].type.classifier as? KClass<*>)?.isSubclassOf(BackendContext::class) == true
            }.singleOrNull()

            val lowering = if (loweringConstructorWithContext != null) {
                loweringConstructorWithContext.call(context, *args) as Lowering
            } else {
                val loweringConstructorWithoutParameters = Lowering::class.constructors.filter { it.parameters.size == 0 }.singleOrNull()
                loweringConstructorWithoutParameters?.call() as Lowering
            }
            assert(lowering != null)
            lowering.lower(source)
            // `source` is modified in place
            return source
        }
    }

fun KClass<*>.isSubclassOf(base: KClass<*>): Boolean =
    if (this == base) true
    else supertypes.mapNotNull { it.classifier as? KClass<*> }.any { it.isSubclassOf(base) }

object IrFileStartPhase : CompilerPhase<BackendContext, IrFile> {
    override val name = "IrFileStart"
    override val description = "State at start of IrFile lowering"
    override val prerequisite = emptySet()
    override fun invoke(_context: BackendContext, source: IrFile) = source
}

object IrFileEndPhase : CompilerPhase<BackendContext, IrFile> {
    override val name = "IrFileEnd"
    override val description = "State at end of IrFile lowering"
    override val prerequisite = emptySet()
    override fun invoke(_context: BackendContext, source: IrFile) = source
}