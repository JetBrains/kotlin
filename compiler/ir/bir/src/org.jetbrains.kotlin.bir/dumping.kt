/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.util.Bir2IrConverter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.dump
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.DurationUnit

val showResultTable = false

private val irDumpOptions = DumpIrTreeOptions(printFlagsInDeclarationReferences = false, printFilePath = false)
private val dumpBir = false
private val dumpIr = false

fun dumpOriginalIrPhase(phaseConfig: PhaseConfigurationService, input: IrModuleFragment, phaseName: String, isBefore: Boolean) {
    if (!dumpIr) return

    var name = phaseName
    phaseConfig.dumpToDirectory?.let { dumpDir ->
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
    phaseConfig: PhaseConfigurationService,
    input: BirCompilation.BirCompilationBundle,
    phase: BirLoweringPhase?,
    phaseName: String?,
) {
    if (!dumpBir) return

    phaseConfig.dumpToDirectory?.let { dumpDir ->
        val irPhases = phase?.let {
            val i = input.backendContext!!.loweringPhases.indexOf(phase)
            allBirPhases[i].second
        }
        val irPhaseName = phaseName ?: irPhases?.lastOrNull() ?: return

        val compiledBir = input.birModule!!.getContainingDatabase()!!
        val bir2IrConverter = Bir2IrConverter(
            input.dynamicPropertyManager!!,
            input.backendContext!!.compressedSourceSpanManager,
            input.mappedIr2BirElements,
            context.irBuiltIns,
            compiledBir,
            input.estimatedIrTreeSize
        )
        bir2IrConverter.reuseOnlyExternalElements = true

        val irModule = bir2IrConverter.remapElement<IrModuleFragment>(input.birModule)

        val text = irModule.dump(irDumpOptions)
        val path = Path(dumpDir) / "bir" / "${irPhaseName}.txt"
        path.createParentDirectories()
        path.writeText(text)
    }
}

fun printCompilationPhaseTime(
    input: BirCompilation.BirCompilationBundle?,
    kind: String?, name: String, time: Duration,
) {
    val label = when (kind) {
        "BIR" -> {
            val i = input?.backendContext?.loweringPhases?.indexOfFirst { it.javaClass.simpleName == name }
            val irPhases = i?.takeUnless { it == -1 }?.let { allBirPhases[it].second }.orEmpty()
            when (irPhases.size) {
                0 -> name
                1 -> irPhases.last()
                else -> name + irPhases.joinToString(", ", " [", "]")
            }
        }
        "IR" -> {
            if (!allBirPhases.any { name in it.second }) {
                return
            }
            name
        }
        else -> name
    }

    println("$kind: $label: ${time.format()}")
}

fun printCompilationTimings(
    input: BirCompilation.BirCompilationBundle,
    irPhasesTime: Map<String, Duration>,
    birPhasesTime: Map<String, Duration>,
) {
    birPhasesTime.forEach { birPhaseName, birTime ->
        val i = input.backendContext!!.loweringPhases.indexOfFirst { it.javaClass.simpleName == birPhaseName }
        val irPhases = i.takeUnless { it == -1 }?.let { allBirPhases[it].second }
        val irPhaseName = irPhases?.lastOrNull()
        val irPhaseTime = irPhasesTime[irPhaseName]

        print(irPhaseName ?: birPhaseName)
        print(" | ")
        if (irPhaseTime != null)
            print(irPhaseTime.format())
        print(" | ")
        print(birTime.format())
        println()
    }
}

fun Duration.format() = toString(DurationUnit.MILLISECONDS, 2).removeSuffix("ms")