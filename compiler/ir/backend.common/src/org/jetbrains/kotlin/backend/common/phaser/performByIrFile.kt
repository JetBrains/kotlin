/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.phaser.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun <Context : LoweringContext> performByIrFile(
    lower: List<SimpleNamedCompilerPhase<Context, IrFile, IrFile>>,
): SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> = PerformByIrFilePhase(lower, supportParallel = false)

class PerformByIrFilePhase<Context : LoweringContext>(
    private val lower: List<SimpleNamedCompilerPhase<Context, IrFile, IrFile>>,
    private val supportParallel: Boolean,
) : SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>(name = "PerformByIrFilePhase") {
    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment,
    ): IrModuleFragment {
        return input
    }

    override fun phaseBody(context: Context, input: IrModuleFragment): IrModuleFragment {
        val nThreads = context.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1
        return if (supportParallel && nThreads > 1)
            invokeParallel(context, input, nThreads)
        else
            invokeSequential(context, input)
    }

    private fun invokeSequential(
        context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        for (irFile in input.files) {
            try {
                for (phase in lower) {
                    phase.phaseBody(context, irFile)
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
        context: Context, input: IrModuleFragment, nThreads: Int
    ): IrModuleFragment {
        if (input.files.isEmpty()) return input

        // We can only report one exception through ISE
        val thrownFromThread = AtomicReference<Pair<Throwable, IrFile>?>(null)

        val executor = Executors.newFixedThreadPool(nThreads)
        for (irFile in input.files) {
            executor.execute {
                try {
                    for (phase in lower) {
                        phase.phaseBody(context, irFile)
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

        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> =
        lower.flatMap { it.getNamedSubphases(startDepth) }
}
