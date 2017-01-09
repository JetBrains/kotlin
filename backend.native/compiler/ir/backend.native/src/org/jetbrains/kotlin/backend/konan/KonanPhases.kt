package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.util.*

enum class KonanPhase(val description: String,
                      var enabled: Boolean = true, var verbose: Boolean = false) {

    /* */ FRONTEND("Frontend builds AST"),
    /* */ PSI_TO_IR("Psi to IR conversion"),
    /* */ BACKEND("All backend"),
    /* ... */ LOWER("IR Lowering"), 
    /* ... ... */ LOWER_BUILTIN_OPERATORS("BuiltIn Operators Lowering"),
    /* ... ... */ LOWER_DEFAULT_PARAMETER_EXTENT("Default Parameter Extent Lowering"),
    /* ... ... */ LOWER_TYPE_OPERATORS("Type operators lowering"),
    /* ... ... */ LOWER_SHARED_VARIABLES("Shared Variable Lowering"),
    /* ... ... */ LOWER_LOCAL_FUNCTIONS("Local Function Lowering"),
    /* ... ... */ LOWER_CALLABLES("Callable references Lowering"),
    /* ... ... */ LOWER_INLINE("Functions inlining"),
    /* ... ... */ AUTOBOX("Autoboxing of primitive types"),
    /* ... */ BITCODE("LLVM BitCode Generation"),
    /* ... ... */ RTTI("RTTI Generation"),
    /* ... ... */ CODEGEN("Code Generation"),
    /* ... ... */ METADATOR("Metadata Generation"),
    /* */ LINKER("Link Stage");
}

object KonanPhases {
    val phases = KonanPhase.values().associate { it.name.toLowerCase() to it }

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -list to see the list of phases.")
        }
        return name
    }

    fun config(config: KonanConfig) {
        with (config.configuration) { with (KonanConfigKeys) { 
            val disabled = get(DISABLED_PHASES)
            disabled?.forEach { phases[known(it)]!!.enabled = false }

            val enabled = get(ENABLED_PHASES)
            enabled?.forEach { phases[known(it)]!!.enabled = true }

            val verbose = get(VERBOSE_PHASES)
            verbose?.forEach { phases[known(it)]!!.verbose = true }

            if (get(NOLINK) ?: false ) {
                KonanPhase.LINKER.enabled = false
            }
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

internal class PhaseManager(val context: Context)  {

    internal fun phase(phase: KonanPhase, body: () -> Unit) {

        if (!phase .enabled) return

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
            if (shouldVerifyBitCode()) {
                verifyBitCode()
            }

            if (shouldPrintDescriptors()) {
                printDescriptors()
            }
            if (shouldPrintIr()) {
                printIr()
            }
            if (shouldPrintBitCode()) {
                printBitCode()
            }
        }

        context.depth --
        context.phase = savePhase
    }
}


