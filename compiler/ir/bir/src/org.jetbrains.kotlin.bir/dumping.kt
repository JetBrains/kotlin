/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
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

private val dumpBir = true
private val dumpIr = true

fun dumpOriginalIrPhase(context: JvmBackendContext, input: IrModuleFragment, phaseName: String, isBefore: Boolean) {
    if (!dumpIr) return

    var name = phaseName
    context.phaseConfig.dumpToDirectory?.let { dumpDir ->
        if (isBefore) {
            if (phaseName == allBirPhases.firstNotNullOfOrNull { it.second.firstOrNull() }) {
                name = "Initial"
            } else {
                return
            }
        } else {
            if (phaseName !in allBirPhases.flatMap { it.second }) {
                return
            }
        }

        val text = input.dump(irDumpOptions)
        val path = Path(dumpDir) / "ir" / "${name}.txt"
        path.createParentDirectories()
        path.writeText(text)
    }
}

fun dumpBirPhase(
    context: JvmBackendContext,
    input: BirCompilationBundle,
    phase: BirLoweringPhase?,
    phaseName: String?,
) {
    if (!dumpBir) return

    context.phaseConfig.dumpToDirectory?.let { dumpDir ->
        val irPhases = phase?.let {
            val i = input.backendContext!!.loweringPhases.indexOf(phase)
            allBirPhases[i].second
        }
        val irPhaseName = phaseName ?: irPhases?.lastOrNull() ?: return

        val compiledBir = input.birModule!!.getContainingForest()!!
        val bir2IrConverter = Bir2IrConverter(
            input.dynamicPropertyManager!!,
            input.mappedIr2BirElements,
            context.irBuiltIns,
            compiledBir,
            input.estimatedIrTreeSize
        )
        bir2IrConverter.reuseOnlyExternalElements = true

        val irModule = bir2IrConverter.remapElement<IrModuleFragment>(input.birModule)
        //irModule.patchDeclarationParents()
        val text = irModule.dump(irDumpOptions)

        val path = Path(dumpDir) / "bir" / "${irPhaseName}.txt"
        path.createParentDirectories()
        path.writeText(text)
    }
}