/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

fun CompilerPhase<*, *, *>.toPhaseMap(): MutableMap<String, AnyNamedPhase> =
    getNamedSubphases().fold(mutableMapOf()) { acc, (_, phase) ->
        check(phase.name !in acc) { "Duplicate phase name '${phase.name}'" }
        acc[phase.name] = phase
        acc
    }

class PhaseConfigBuilder(private val compoundPhase: CompilerPhase<*, *, *>) {
    val enabled = mutableSetOf<AnyNamedPhase>()
    val verbose = mutableSetOf<AnyNamedPhase>()
    val toDumpStateBefore = mutableSetOf<AnyNamedPhase>()
    val toDumpStateAfter = mutableSetOf<AnyNamedPhase>()
    var dumpToDirectory: String? = null
    var dumpOnlyFqName: String? = null
    val toValidateStateBefore = mutableSetOf<AnyNamedPhase>()
    val toValidateStateAfter = mutableSetOf<AnyNamedPhase>()
    var needProfiling = false
    var checkConditions = false
    var checkStickyConditions = false

    fun build() = PhaseConfig(
        compoundPhase, compoundPhase.toPhaseMap(), enabled,
        verbose, toDumpStateBefore, toDumpStateAfter, dumpToDirectory, dumpOnlyFqName,
        toValidateStateBefore, toValidateStateAfter,
        needProfiling, checkConditions, checkStickyConditions
    )
}

/**
 * Phase configuration that defines and configures [CompilerPhase]s that the compiler should execute.
 * It is defined before compilation and can't be modified in the process.
 */
class PhaseConfig(
    private val compoundPhase: CompilerPhase<*, *, *>,
    private val phases: Map<String, AnyNamedPhase> = compoundPhase.toPhaseMap(),
    private val initiallyEnabled: Set<AnyNamedPhase> = phases.values.toSet(),
    val verbose: Set<AnyNamedPhase> = emptySet(),
    val toDumpStateBefore: Set<AnyNamedPhase> = emptySet(),
    val toDumpStateAfter: Set<AnyNamedPhase> = emptySet(),
    override val dumpToDirectory: String? = null,
    override val dumpOnlyFqName: String? = null,
    private val toValidateStateBefore: Set<AnyNamedPhase> = emptySet(),
    private val toValidateStateAfter: Set<AnyNamedPhase> = emptySet(),
    override val needProfiling: Boolean = false,
    override val checkConditions: Boolean = false,
    override val checkStickyConditions: Boolean = false
) : PhaseConfigurationService {
    @Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        compoundPhase: CompilerPhase<*, *, *>,
        phases: Map<String, AnyNamedPhase> = compoundPhase.toPhaseMap(),
        initiallyEnabled: Set<AnyNamedPhase> = phases.values.toSet(),
        verbose: Set<AnyNamedPhase> = emptySet(),
        toDumpStateBefore: Set<AnyNamedPhase> = emptySet(),
        toDumpStateAfter: Set<AnyNamedPhase> = emptySet(),
        dumpToDirectory: String? = null,
        dumpOnlyFqName: String? = null,
        toValidateStateBefore: Set<AnyNamedPhase> = emptySet(),
        toValidateStateAfter: Set<AnyNamedPhase> = emptySet(),
        @Suppress("UNUSED_PARAMETER") namesOfElementsExcludedFromDumping: Set<String> = emptySet(),
        needProfiling: Boolean = false,
        checkConditions: Boolean = false,
        checkStickyConditions: Boolean = false,
    ) : this(
        compoundPhase, phases, initiallyEnabled, verbose, toDumpStateBefore, toDumpStateAfter, dumpToDirectory, dumpOnlyFqName,
        toValidateStateBefore, toValidateStateAfter, needProfiling, checkConditions, checkStickyConditions
    )

    fun toBuilder() = PhaseConfigBuilder(compoundPhase).also {
        it.enabled.addAll(initiallyEnabled)
        it.verbose.addAll(verbose)
        it.toDumpStateBefore.addAll(toDumpStateBefore)
        it.toDumpStateAfter.addAll(toDumpStateAfter)
        it.dumpToDirectory = dumpToDirectory
        it.dumpOnlyFqName = dumpOnlyFqName
        it.toValidateStateBefore.addAll(toValidateStateBefore)
        it.toValidateStateAfter.addAll(toValidateStateAfter)
        it.needProfiling = needProfiling
        it.checkConditions = checkConditions
        it.checkStickyConditions = checkStickyConditions
    }

    override fun isEnabled(phase: AnyNamedPhase): Boolean =
        phase in enabled

    override fun isVerbose(phase: AnyNamedPhase): Boolean =
        phase in verbose

    override fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateBefore

    override fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateAfter

    override fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateBefore

    override fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateAfter

    private val enabledMut = initiallyEnabled.toMutableSet()

    val enabled: Set<AnyNamedPhase> get() = enabledMut

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -Xlist-phases to see the list of phases.")
        }
        return name
    }

    fun list() {
        compoundPhase.getNamedSubphases().forEach { (depth, phase) ->
            val disabled = if (phase !in enabled) " (Disabled)" else ""
            val verbose = if (phase in verbose) " (Verbose)" else ""

            println(
                "%1$-50s %2$-50s %3$-10s".format(
                    "${"    ".repeat(depth)}${phase.name}", phase.description, "$disabled$verbose"
                )
            )
        }
    }

    fun enable(phase: AnyNamedPhase) {
        enabledMut.add(phase)
    }

    override fun disable(phase: AnyNamedPhase) {
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
