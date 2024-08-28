/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.producesMetadata
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun <Context : CommonBackendContext> performByIrFile(
    name: String,
    lower: List<CompilerPhase<Context, IrFile, IrFile>>,
    supportParallel: Boolean,
): SameTypeNamedCompilerPhase<Context, IrModuleFragment> =
    SameTypeNamedCompilerPhase(
        name, emptySet(), PerformByIrFilePhase(lower, supportParallel), emptySet(), emptySet(), emptySet(),
        setOf(getIrDumper()), nlevels = 1,
    )

private class PerformByIrFilePhase<Context : CommonBackendContext>(
    private val lower: List<CompilerPhase<Context, IrFile, IrFile>>,
    private val supportParallel: Boolean,
) : SameTypeCompilerPhase<Context, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<IrModuleFragment>,
        context: Context,
        input: IrModuleFragment
    ): IrModuleFragment {
        val nThreads = context.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1
        return if (supportParallel && nThreads > 1)
            invokeParallel(phaseConfig, phaserState, context, input, nThreads)
        else
            invokeSequential(phaseConfig, phaserState, context, input)
    }

    private fun invokeSequential(
        phaseConfig: PhaseConfigurationService, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        for (irFile in input.files) {
            if (irFile.producesMetadata) {
                continue
            }
            try {
                val filePhaserState = phaserState.changePhaserStateType<IrModuleFragment, IrFile>()
                for (phase in lower) {
                    phase.invoke(phaseConfig, filePhaserState, context, irFile)
                }
            } catch (e: Throwable) {
                CodegenUtil.reportBackendException(e, "IR lowering", irFile.fileEntry.name) { offset ->
                    irFile.fileEntry.takeIf { it.supportsDebugInfo }?.let {
                        val (line, column) = it.getLineAndColumnNumbers(offset)
                        line to column
                    }
                }
            }
        }

        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    private fun invokeParallel(
        phaseConfig: PhaseConfigurationService, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment, nThreads: Int
    ): IrModuleFragment {
        if (input.files.isEmpty()) return input

        // We can only report one exception through ISE
        val thrownFromThread = AtomicReference<Pair<Throwable, IrFile>?>(null)

        // Each thread needs its own copy of phaserState.alreadyDone
        val filesAndStates = input.files.map {
            it to phaserState.copyOf()
        }

        val executor = Executors.newFixedThreadPool(nThreads)
        for ((irFile, state) in filesAndStates) {
            executor.execute {
                try {
                    val filePhaserState = state.changePhaserStateType<IrModuleFragment, IrFile>()
                    for (phase in lower) {
                        phase.invoke(phaseConfig, filePhaserState, context, irFile)
                    }
                } catch (e: Throwable) {
                    thrownFromThread.set(Pair(e, irFile))
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.DAYS) // Wait long enough

        thrownFromThread.get()?.let { (e, irFile) ->
            CodegenUtil.reportBackendException(e, "Experimental parallel IR backend", irFile.fileEntry.name) { offset ->
                irFile.fileEntry.takeIf { it.supportsDebugInfo }?.let {
                    val (line, column) = it.getLineAndColumnNumbers(offset)
                    line to column
                }
            }
        }

        // Presumably each thread has run through the same list of phases.
        phaserState.alreadyDone.addAll(filesAndStates[0].second.alreadyDone)

        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, AbstractNamedCompilerPhase<Context, *, *>>> =
        lower.flatMap { it.getNamedSubphases(startDepth) }
}
