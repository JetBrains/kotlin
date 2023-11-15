/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.backend.common.phaser.AnyNamedPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.util.Bir2IrConverter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

private val irDumpOptions = DumpIrTreeOptions(printFlagsInDeclarationReferences = false, printFilePath = false)
private val dumpBir = false
private val dumpIr = false

fun dumpOriginalIrPhase(context: JvmBackendContext, input: IrModuleFragment, phase: AnyNamedPhase) {
    if (!dumpIr) return

    context.phaseConfig.dumpToDirectory?.let { dumpDir ->
        if (phase.name !in allBirPhases.flatMap { it.second }) {
            return
        }

        val text = input.dump(irDumpOptions)
        val path = Path(dumpDir) / "ir" / "${phase.name}.txt"
        path.createParentDirectories()
        path.writeText(text)
    }
}

fun dumpBirPhase(
    context: JvmBackendContext,
    input: BirCompilationBundle,
    phase: BirLoweringPhase,
) {
    if (!dumpBir) return

    context.phaseConfig.dumpToDirectory?.let { dumpDir ->
        val i = input.backendContext.loweringPhases.indexOf(phase)
        val irPhases = allBirPhases[i].second
        val irPhaseName = irPhases.lastOrNull() ?: return

        val compiledBir = input.birModule.getContainingForest()!!
        val bir2IrConverter = Bir2IrConverter(
            input.dynamicPropertyManager,
            input.externalIr2BirElements,
            context.irBuiltIns,
            compiledBir,
            input.estimatedIrTreeSize
        )

        val irModule = bir2IrConverter.remapElement<IrModuleFragment>(input.birModule)
        irModule.patchDeclarationParents()
        val text = irModule.dump(irDumpOptions)

        val path = Path(dumpDir) / "bir" / "${irPhaseName}.txt"
        path.createParentDirectories()
        path.writeText(text)
    }
}