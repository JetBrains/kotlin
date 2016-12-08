package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.Context

class KonanPhase (val description: String, 
    var enabled: Boolean = true, var verbose: Boolean = false) {
}

object KonanPhases {
    val phases = mapOf<String, KonanPhase> (
        "Backend" to KonanPhase("All backend"),
        "Optimizer" to KonanPhase("IR Optimizer"),
        "Lower_shared_variables" to KonanPhase("Shared Variable Lowering"),
        "Lower_local_functions" to KonanPhase("Local Function Lowering"),
        "Lower_callables" to KonanPhase("Callable references Lowering"),
        "Lower" to KonanPhase("IR Lowering"),
        "Bitcode" to KonanPhase("LLVM BitCode Generation"),
        "RTTI" to KonanPhase("RTTI Generation"),
        "Codegen" to KonanPhase("Code Generation"),
        "Metadator" to KonanPhase("Metadata Generation"),
        "Linker" to KonanPhase("Link Stage")
    )

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

    internal fun phase(shortName: String, body: () -> Unit) {

        val phase = KonanPhases.phases[shortName]
        if (phase == null) throw Error("Unknown backend phase: $shortName")
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


