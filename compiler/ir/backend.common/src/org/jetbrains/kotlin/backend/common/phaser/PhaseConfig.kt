package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

class PhaseConfig(private val compoundPhase: CompilerPhase<*, *, *>, config: CompilerConfiguration) {

    val phases = compoundPhase.getNamedSubphases().map { (_, phase) -> phase }.associate { it.name to it }
    private val enabledMut = computeEnabled(config).toMutableSet()

    val enabled: Set<AnyNamedPhase> get() = enabledMut

    val verbose = phaseSetFromConfiguration(config, CommonConfigurationKeys.VERBOSE_PHASES)
    val toDumpStateBefore: Set<AnyNamedPhase>

    val toDumpStateAfter: Set<AnyNamedPhase>
    val toValidateStateBefore: Set<AnyNamedPhase>

    val toValidateStateAfter: Set<AnyNamedPhase>

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

    val needProfiling = config.getBoolean(CommonConfigurationKeys.PROFILE_PHASES)
    val checkConditions = config.getBoolean(CommonConfigurationKeys.CHECK_PHASE_CONDITIONS)
    val checkStickyConditions = config.getBoolean(CommonConfigurationKeys.CHECK_STICKY_CONDITIONS)

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -Xlist-phases to see the list of phases.")
        }
        return name
    }

    fun list() {
        compoundPhase.getNamedSubphases().forEach { (depth, phase) ->
            val enabled = if (phase in enabled) "(Enabled)" else ""
            val verbose = if (phase in verbose) "(Verbose)" else ""

            println(String.format("%1$-50s %2$-50s %3$-10s", "${"\t".repeat(depth)}${phase.name}:", phase.description, "$enabled $verbose"))
        }
    }

    private fun computeEnabled(config: CompilerConfiguration) =
            with(CommonConfigurationKeys) {
                val disabledPhases = phaseSetFromConfiguration(config, DISABLED_PHASES)
                phases.values.toSet() - disabledPhases
            }

    private fun phaseSetFromConfiguration(config: CompilerConfiguration, key: CompilerConfigurationKey<Set<String>>): Set<AnyNamedPhase> {
        val phaseNames = config.get(key) ?: emptySet()
        if ("ALL" in phaseNames) return phases.values.toSet()
        return phaseNames.map { phases[it]!! }.toSet()
    }

    fun enable(phase: AnyNamedPhase) {
        enabledMut.add(phase)
    }

    fun disable(phase: AnyNamedPhase) {
        enabledMut.remove(phase)
    }
    fun switch(phase: AnyNamedPhase, onOff: Boolean) {
        if (onOff) {
            enable(phase)
        } else {
            disable(phase)
        }
    }
}
