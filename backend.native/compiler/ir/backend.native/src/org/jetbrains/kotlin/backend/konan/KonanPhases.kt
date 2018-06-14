/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

enum class KonanPhase(val description: String,
                      vararg prerequisite: KonanPhase,
                      var enabled: Boolean = true,
                      var verbose: Boolean = false) {
    /* */ FRONTEND("Frontend builds AST"),
    /* */ PSI_TO_IR("Psi to IR conversion"),
    /* */ IR_GENERATOR_PLUGINS("Plugged-in ir generators"),
    /* */ GEN_SYNTHETIC_FIELDS("Generate synthetic fields"),
    /* */ COPY_DEFAULT_VALUES_TO_ACTUAL("Copy default values from expect to actual declarations"),
    /* */ SERIALIZER("Serialize descriptor tree and inline IR bodies", GEN_SYNTHETIC_FIELDS),
    /* */ BACKEND("All backend"),
    /* ... */ LOWER("IR Lowering"),
    /* ... ... */ REMOVE_EXPECT_DECLARATIONS("Expect declarations removing"),
    /* ... ... */ TEST_PROCESSOR("Unit test processor"),
    /* ... ... */ LOWER_BEFORE_INLINE("Special operations processing before inlining"),
    /* ... ... */ LOWER_INLINE("Functions inlining", LOWER_BEFORE_INLINE),
    /* ... ... */ LOWER_AFTER_INLINE("Special operations processing after inlining"),
    /* ... ... ...  */ DESERIALIZER("Deserialize inline bodies"),
    /* ... ... */ LOWER_INTEROP_PART1("Interop lowering, part 1", LOWER_INLINE),
    /* ... ... */ LOWER_FOR_LOOPS("For loops lowering"),
    /* ... ... */ LOWER_ENUMS("Enum classes lowering"),
    /* ... ... */ LOWER_DELEGATION("Delegation lowering"),
    /* ... ... */ LOWER_INITIALIZERS("Initializers lowering", LOWER_ENUMS),
    /* ... ... */ LOWER_LATEINIT("Lateinit properties lowering", LOWER_INLINE),
    /* ... ... */ LOWER_SHARED_VARIABLES("Shared Variable Lowering", LOWER_INITIALIZERS),
    /* ... ... */ LOWER_CALLABLES("Callable references Lowering", LOWER_DELEGATION),
    /* ... ... */ LOWER_LOCAL_FUNCTIONS("Local Function Lowering", LOWER_SHARED_VARIABLES, LOWER_CALLABLES),
    /* ... ... */ LOWER_INTEROP_PART2("Interop lowering, part 2", LOWER_LOCAL_FUNCTIONS),
    /* ... ... */ LOWER_TAILREC("tailrec lowering", LOWER_LOCAL_FUNCTIONS),
    /* ... ... */ LOWER_FINALLY("Finally blocks lowering", LOWER_INITIALIZERS, LOWER_LOCAL_FUNCTIONS, LOWER_TAILREC),
    /* ... ... */ LOWER_DEFAULT_PARAMETER_EXTENT("Default Parameter Extent Lowering", LOWER_TAILREC, LOWER_ENUMS),
    /* ... ... */ LOWER_VARARG("Vararg lowering", LOWER_CALLABLES, LOWER_DEFAULT_PARAMETER_EXTENT),
    /* ... ... */ LOWER_COMPILE_TIME_EVAL("Compile time evaluation lowering", LOWER_VARARG, enabled = false),
    /* ... ... */ LOWER_INNER_CLASSES("Inner classes lowering", LOWER_DEFAULT_PARAMETER_EXTENT, GEN_SYNTHETIC_FIELDS),
    /* ... ... */ LOWER_BUILTIN_OPERATORS("BuiltIn Operators Lowering", LOWER_DEFAULT_PARAMETER_EXTENT),
    /* ... ... */ LOWER_COROUTINES("Coroutines lowering", LOWER_LOCAL_FUNCTIONS),
    /* ... ... */ LOWER_TYPE_OPERATORS("Type operators lowering", LOWER_COROUTINES),
    /* ... ... */ BRIDGES_BUILDING("Bridges building", LOWER_COROUTINES),
    /* ... ... */ LOWER_STRING_CONCAT("String concatenation lowering"),
    /* ... ... */ LOWER_DATA_CLASSES("Data classes lowering"),
    /* ... ... */ AUTOBOX("Autoboxing of primitive types", BRIDGES_BUILDING, LOWER_COROUTINES),
    /* ... ... */ RETURNS_INSERTION("Returns insertion for Unit functions", AUTOBOX, LOWER_COROUTINES, LOWER_ENUMS),
    /* ... */ BITCODE("LLVM BitCode Generation"),
    /* ... ... */ RTTI("RTTI Generation"),
    /* ... ... */ BUILD_DFG("Data flow graph building", enabled = false),
    /* ... ... */ DESERIALIZE_DFG("Data flow graph deserializing", enabled = false),
    /* ... ... */ DEVIRTUALIZATION("Devirtualization", BUILD_DFG, DESERIALIZE_DFG, enabled = false),
    /* ... ... */ ESCAPE_ANALYSIS("Escape analysis", BUILD_DFG, DESERIALIZE_DFG, enabled = false), // TODO: Requires devirtualization.
    /* ... ... */ SERIALIZE_DFG("Data flow graph serializing", BUILD_DFG, enabled = false), // TODO: Requires escape analysis.
    /* ... ... */ CODEGEN("Code Generation"),
    /* ... ... */ C_STUBS("C stubs compilation"),
    /* ... ... */ BITCODE_LINKER("Bitcode linking"),
    /* */ LINK_STAGE("Link stage"),
    /* ... */ OBJECT_FILES("Bitcode to object file"),
    /* ... */ LINKER("Linker");

    val prerequisite = prerequisite.toSet()
}

object KonanPhases {
    val phases = KonanPhase.values().associate { it.visibleName to it }

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use --list_phases to see the list of phases.")
        }
        return name
    }

    fun config(config: KonanConfig) {
        with (config.configuration) { with (KonanConfigKeys) {

            // Don't serialize anything to a final executable.
            KonanPhase.SERIALIZER.enabled =
                (config.produce == CompilerOutputKind.LIBRARY)
            KonanPhase.LINK_STAGE.enabled = config.produce.isNativeBinary

            KonanPhase.TEST_PROCESSOR.enabled =
                    getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE

            val disabled = get(DISABLED_PHASES)
            disabled?.forEach { phases[known(it)]!!.enabled = false }

            val enabled = get(ENABLED_PHASES)
            enabled?.forEach { phases[known(it)]!!.enabled = true }

            val verbose = get(VERBOSE_PHASES)
            verbose?.forEach { phases[known(it)]!!.verbose = true }
        }}
    }

    fun list() {
        phases.forEach { key, phase ->
            val enabled = if (phase.enabled) "(Enabled)" else ""
            val verbose = if (phase.verbose) "(Verbose)" else ""

            println(String.format("%1$-30s%2$-30s%3$-10s", "${key}:", phase.description, "$enabled $verbose"))
        }
    }
}

internal class PhaseManager(val context: Context, val parent: PhaseManager? = null)  {

    val previousPhases = mutableSetOf<KonanPhase>()

    fun createChild() = PhaseManager(context, this)

    private fun checkPrerequisite(phase: KonanPhase): Boolean =
            previousPhases.contains(phase) || parent?.checkPrerequisite(phase) == true

    fun phase(phase: KonanPhase, body: () -> Unit) {

        if (!phase.enabled) return

        phase.prerequisite.forEach {
            if (!checkPrerequisite(it))
                throw Error("$phase requires $it")
        }

        previousPhases.add(phase)

        val savePhase = context.phase
        context.phase = phase
        context.depth ++

        with (context) {
            profileIf(shouldProfilePhases(), "Phase ${nTabs(depth)} ${phase.name}") {
                body()
            }

            if (shouldVerifyDescriptors()) {
                verifyDescriptors()
            }
            if (shouldVerifyIr()) {
                verifyIr()
            }

            if (shouldPrintDescriptors()) {
                printDescriptors()
            }
            if (shouldPrintIr()) {
                printIr()
            }
            if (shouldPrintIrWithDescriptors()) {
                printIrWithDescriptors()
            }
            if (shouldPrintLocations()) {
                printLocations()
            }
        }

        context.depth --
        context.phase = savePhase
    }
}


