/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.lower.BirJvmStaticInObjectLowering
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import kotlin.system.exitProcess

private val excludedStandardPhases = setOf<String>()

fun lowerWithBir(
    phases: SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment>,
    context: JvmBackendContext,
    irModuleFragment: IrModuleFragment,
) {
    val standardPhases = phases.getNamedSubphases()
    val newPhases = standardPhases
        .filter { it.first == 1 }
        .map { it.second }
        .toMutableList() as MutableList<CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>>

    val idx = newPhases.indexOfFirst { (it as AnyNamedPhase).name == "FileClass" }
    newPhases.add(idx + 1, invokeBirLoweringsPhase)

    val compoundPhase = newPhases.reduce { result, phase -> result then phase }
    val phaseConfig = PhaseConfig(compoundPhase)

    val standardPhasesByName = standardPhases.associate { it.second.name to it.second }
    excludedStandardPhases.forEach {
        val phase = standardPhasesByName[it] as AnyNamedPhase
        phaseConfig.disable(phase)
    }

    compoundPhase.invokeToplevel(phaseConfig, context, irModuleFragment)
}

private val invokeBirLoweringsPhase = SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment>(
    "!!!BIR run lowerings",
    "Experimental phase to test alternative IR architecture",
    lower = BirLowering,
)

private val birPhases = listOf<(JvmBirBackendContext) -> BirLoweringPhase>(
    ::BirJvmStaticInObjectLowering
)

private object BirLowering : SameTypeCompilerPhase<JvmBackendContext, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<IrModuleFragment>,
        context: JvmBackendContext,
        input: IrModuleFragment,
    ): IrModuleFragment {
        val dynamicPropertyManager = BirElementDynamicPropertyManager()
        val birForest = BirForest()
        val ir2BirConverter = Ir2BirConverter(dynamicPropertyManager)
        ir2BirConverter.birForest = birForest
        val birContext = JvmBirBackendContext(
            context,
            input.descriptor,
            birForest,
            ir2BirConverter,
            dynamicPropertyManager,
            birPhases,
        )
        val birModule = ir2BirConverter.remapElement<BirModuleFragment>(input)

        countElements(birModule)

        birForest.applyNewRegisteredIndices()
        birForest.reindexAllElements()

        for (phase in birContext.loweringPhases) {
            println("Running BIR phase ${phase.javaClass.name}")
            phase(birModule)
        }

        exitProcess(0)
        return input
    }

    private fun countElements(root: BirElement): Int {
        var count = 0
        root.accept {
            count++
            it.walkIntoChildren()
        }
        return count
    }
}
