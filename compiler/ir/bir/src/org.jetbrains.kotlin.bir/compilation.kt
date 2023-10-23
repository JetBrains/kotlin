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
import org.jetbrains.kotlin.bir.backend.lower.BirRepeatedAnnotationLowering
import org.jetbrains.kotlin.bir.declarations.BirExternalPackageFragment
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
    val birPhases = listOf<AbstractNamedCompilerPhase<JvmBackendContext, *, *>>(
        ConvertIrToBirPhase(
            "Convert IR to BIR",
            "Experimental phase to test alternative IR architecture",
        ),
        NamedCompilerPhase(
            "Run BIR lowerings",
            "Experimental phase to test alternative IR architecture",
            lower = BirLowering,
        )
    )
    newPhases.addAll(idx + 1, birPhases as List<NamedCompilerPhase<JvmBackendContext, IrModuleFragment>>)

    val compoundPhase = newPhases.reduce { result, phase -> result then phase }
    val phaseConfig = PhaseConfig(compoundPhase)

    val standardPhasesByName = standardPhases.associate { it.second.name to it.second }
    excludedStandardPhases.forEach {
        val phase = standardPhasesByName[it] as AnyNamedPhase
        phaseConfig.disable(phase)
    }

    compoundPhase.invokeToplevel(phaseConfig, context, irModuleFragment)
}

private val birPhases = listOf<(JvmBirBackendContext) -> BirLoweringPhase>(
    ::BirJvmStaticInObjectLowering,
    ::BirRepeatedAnnotationLowering,
)

private class ConvertIrToBirPhase(name: String, description: String) :
    SimpleNamedCompilerPhase<JvmBackendContext, IrModuleFragment, BirCompilationBundle>(name, description) {
    override fun phaseBody(context: JvmBackendContext, input: IrModuleFragment): BirCompilationBundle {
        val dynamicPropertyManager = BirElementDynamicPropertyManager()

        val externalDependenciesBir = BirForest()
        val compiledBir = BirForest()

        val ir2BirConverter = Ir2BirConverter(dynamicPropertyManager)
        ir2BirConverter.copyAncestorsForOrphanedElements = true
        ir2BirConverter.appendElementAsForestRoot = { old, new ->
            when {
                old === input -> compiledBir
                new is BirModuleFragment || new is BirExternalPackageFragment -> externalDependenciesBir
                else -> null
            }
        }

        val birContext = JvmBirBackendContext(
            context,
            input.descriptor,
            compiledBir,
            ir2BirConverter,
            dynamicPropertyManager,
            birPhases,
        )

        val birModule = ir2BirConverter.remapElement<BirModuleFragment>(input)

        countElements(birModule)

        return BirCompilationBundle(birModule, birContext)
    }


    private fun countElements(root: BirElement): Int {
        var count = 0
        root.accept {
            count++
            it.walkIntoChildren()
        }
        return count
    }

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<IrModuleFragment>,
        context: JvmBackendContext,
        input: IrModuleFragment,
    ): BirCompilationBundle {
        error("Must be enabled")
    }
}

private class BirCompilationBundle(val birModule: BirModuleFragment, val backendContext: JvmBirBackendContext)

private object BirLowering : SameTypeCompilerPhase<JvmBackendContext, BirCompilationBundle> {
    override fun invoke(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<BirCompilationBundle>,
        context: JvmBackendContext,
        input: BirCompilationBundle,
    ): BirCompilationBundle {
        val compiledBir = input.backendContext.compiledBir
        compiledBir.applyNewRegisteredIndices()
        compiledBir.reindexAllElements()

        for (phase in input.backendContext.loweringPhases) {
            println("Running BIR phase ${phase.javaClass.name}")
            phase(input.birModule)
        }

        exitProcess(0)
        return input
    }
}
