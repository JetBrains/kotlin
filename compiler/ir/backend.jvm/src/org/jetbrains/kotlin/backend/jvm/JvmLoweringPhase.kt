/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.dump
import kotlin.system.measureTimeMillis

enum class BeforeOrAfter { BEFORE, AFTER }

object JvmPhaseRunner : PhaseRunner<JvmBackendContext, IrFile> {
    override fun reportBefore(phase: CompilerPhase<JvmBackendContext, IrFile>, depth: Int, context: JvmBackendContext, data: IrFile) {
        if (phase in context.phases.toDumpStateBefore) {
            dumpFile(data, phase, BeforeOrAfter.BEFORE)
        }
    }

    override fun runBody(phase: CompilerPhase<JvmBackendContext, IrFile>, context: JvmBackendContext, source: IrFile): IrFile {
        val runner = when {
            phase === IrFileStartPhase -> ::justRun
            phase === IrFileEndPhase -> ::justRun
            (context.state.configuration.get(CommonConfigurationKeys.PROFILE_PHASES) == true) -> ::runAndProfile
            else -> ::justRun
        }

        context.inVerbosePhase = (phase in context.phases.verbose)

        val result = runner(phase, context, source)

        context.inVerbosePhase = false

        return result
    }

    override fun reportAfter(phase: CompilerPhase<JvmBackendContext, IrFile>, depth: Int, context: JvmBackendContext, data: IrFile) {
        if (phase in context.phases.toDumpStateAfter) {
            dumpFile(data, phase, BeforeOrAfter.AFTER)
        }
    }
}

private fun runAndProfile(phase: CompilerPhase<JvmBackendContext, IrFile>, context: JvmBackendContext, source: IrFile): IrFile {
    var result: IrFile = source
    val msec = measureTimeMillis { result = phase.invoke(context, source) }
    println("${phase.description}: $msec msec")
    return result
}

private fun justRun(phase: CompilerPhase<JvmBackendContext, IrFile>, context: JvmBackendContext, source: IrFile) =
    phase.invoke(context, source)

private fun separator(title: String) {
    println("\n\n--- $title ----------------------\n")
}

private fun dumpFile(irFile: IrFile, phase: CompilerPhase<JvmBackendContext, IrFile>, beforeOrAfter: BeforeOrAfter) {
    // Exclude nonsensical combinations
    if (phase === IrFileStartPhase && beforeOrAfter == BeforeOrAfter.AFTER) return
    if (phase === IrFileEndPhase && beforeOrAfter == BeforeOrAfter.BEFORE) return

    val title = when (phase) {
        IrFileStartPhase -> "IR for ${irFile.name} at the start of lowering process"
        IrFileEndPhase -> "IR for ${irFile.name} at the end of lowering process"
        else -> {
            val beforeOrAfterStr = beforeOrAfter.name.toLowerCase()
            "IR for ${irFile.name} $beforeOrAfterStr ${phase.description}"
        }
    }
    separator(title)
    println(irFile.dump())
}