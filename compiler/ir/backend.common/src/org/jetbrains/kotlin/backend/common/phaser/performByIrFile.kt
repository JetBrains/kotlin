/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.config.phaser.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

fun <Context : LoweringContext> performByIrFile(
    lower: List<SimpleNamedCompilerPhase<Context, IrFile, IrFile>>,
): SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> = PerformByIrFilePhase(lower)

class PerformByIrFilePhase<Context : LoweringContext>(
    private val lower: List<SimpleNamedCompilerPhase<Context, IrFile, IrFile>>,
) : SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>(name = "PerformByIrFilePhase") {
    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment,
    ): IrModuleFragment {
        return input
    }

    override fun phaseBody(context: Context, input: IrModuleFragment): IrModuleFragment {
        shouldNotBeCalled()
    }

    override fun invoke(
        phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        for (irFile in input.files) {
            try {
                for (phase in lower) {
                    phase.invoke(phaseConfig, phaserState, context, irFile)
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


    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> =
        lower.flatMap { it.getNamedSubphases(startDepth) }
}
