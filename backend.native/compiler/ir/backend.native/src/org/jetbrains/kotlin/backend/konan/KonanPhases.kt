package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.Context

enum class KonanPhase(val description: String,
                      var enabled: Boolean = true, var verbose: Boolean = false) {

    /* */ PSI_TO_IR("Psi to IR conversion"),
    /* */ BACKEND("All backend"),
    /* ... */ LOWER("IR Lowering"), 
    /* ... ... */ LOWER_BUILTIN_OPERATORS("BuiltIn Operators Lowering"),
    /* ... ... */ LOWER_SHARED_VARIABLES("Shared Variable Lowering"),
    /* ... ... */ LOWER_LOCAL_FUNCTIONS("Local Function Lowering"),
    /* ... ... */ LOWER_CALLABLES("Callable references Lowering"),
    /* ... ... */ AUTOBOX("Autoboxing of primitive types"),
    /* ... */ BITCODE("LLVM BitCode Generation"),
    /* ... ... */ RTTI("RTTI Generation"),
    /* ... ... */ CODEGEN("Code Generation"),
    /* ... ... */ METADATOR("Metadata Generation"),
    /* */ LINKER("Link Stage");
}

object KonanPhases {
    val phases = KonanPhase.values().associate { it.name to it }

    fun config(config: KonanConfig) {
        val disabled = config.configuration.get(KonanConfigKeys.DISABLED_PHASES)
        disabled?.forEach { phases[it]!!.enabled = false }

        val enabled = config.configuration.get(KonanConfigKeys.ENABLED_PHASES)
        enabled?.forEach { phases[it]!!.enabled = true }

        val verbose = config.configuration.get(KonanConfigKeys.VERBOSE_PHASES)
        verbose?.forEach { phases[it]!!.verbose = true }
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

        body()

        with (context) {
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

        context.phase = savePhase
    }
}


