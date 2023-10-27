/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.lower.*
import org.jetbrains.kotlin.bir.declarations.BirExternalPackageFragment
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.transformFlat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

private val birPhases = listOf<(JvmBirBackendContext) -> BirLoweringPhase>(
    ::BirJvmNameLowering,
    ::BirJvmStaticInObjectLowering,
    ::BirRepeatedAnnotationLowering,
    ::BirTypeAliasAnnotationMethodsLowering,
    ::BirProvisionalFunctionExpressionLowering,
    ::BirJvmOverloadsAnnotationLowering,
    ::BirMainMethodGenerationLowering,
    ::BirAnnotationLowering,
    ::BirPolymorphicSignatureLowering,
    ::BirVarargLowering,
    ::BirJvmLateinitLowering,
)

private val excludedStandardPhases = setOf<String>()

fun lowerWithBir(
    phases: SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment>,
    context: JvmBackendContext,
    irModuleFragment: IrModuleFragment,
) {
    val newPhases = reconstructPhases(phases, context.phaseConfig.needProfiling)

    val birPhases = listOf<AbstractNamedCompilerPhase<JvmBackendContext, *, *>>(
        ConvertIrToBirPhase(
            "Convert IR to BIR",
            "Experimental phase to test alternative IR architecture",
            context.phaseConfig.needProfiling,
        ),
        NamedCompilerPhase(
            "Run BIR lowerings",
            "Experimental phase to test alternative IR architecture",
            lower = BirLowering,
        ),
        ConvertBirToIrPhase(
            "Convert lowered BIR back to IR",
            "Experimental phase to test alternative IR architecture",
        ),
    )
    newPhases.addAll(
        newPhases.indexOfFirst { (it as AnyNamedPhase).name == "FileClass" } + 1,
        birPhases as List<NamedCompilerPhase<JvmBackendContext, IrModuleFragment>>
    )

    val compoundPhase = newPhases.reduce { result, phase -> result then phase }
    val phaseConfig = PhaseConfigBuilder(compoundPhase).apply {
        enabled += compoundPhase.toPhaseMap().values.toSet()
        verbose += context.phaseConfig.verbose
        toDumpStateBefore += context.phaseConfig.toDumpStateBefore
        toDumpStateAfter += context.phaseConfig.toDumpStateAfter
        dumpToDirectory = context.phaseConfig.dumpToDirectory
        dumpOnlyFqName = context.phaseConfig.dumpOnlyFqName
        checkConditions = context.phaseConfig.checkConditions
        checkStickyConditions = context.phaseConfig.checkStickyConditions
    }.build()

    val standardPhasesByName = newPhases.associateBy { (it as AnyNamedPhase).name }
    excludedStandardPhases.forEach {
        val phase = standardPhasesByName[it] as AnyNamedPhase
        phaseConfig.disable(phase)
    }

    compoundPhase.invokeToplevel(phaseConfig, context, irModuleFragment)
}

private fun reconstructPhases(
    phases: SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment>,
    profile: Boolean,
): MutableList<CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>> {
    val standardPhases = phases.getNamedSubphases()
    val newPhases = standardPhases
        .filter { it.first == 1 }
        .map { it.second }
        .toMutableList() as MutableList<NamedCompilerPhase<JvmBackendContext, IrModuleFragment>>

    newPhases.transformFlat { topPhase ->
        if (topPhase.name == "PerformByIrFile") {
            val filePhases = topPhase.getNamedSubphases()
                .filter { it.first == 1 }
                .map { it.second as NamedCompilerPhase<JvmBackendContext, IrFile> }
                .toMutableList()

            filePhases.add(
                filePhases.indexOfFirst { (it as AnyNamedPhase).name == "SuspendLambda" } + 1,
                terminateProcessPhase
            )

            val lower = CustomPerFileAggregateLoweringPhase(filePhases, profile)
            listOf(NamedCompilerPhase(topPhase.name, topPhase.description, lower = lower))
        } else {
            val lower = object : SameTypeCompilerPhase<JvmBackendContext, IrModuleFragment> {
                override fun invoke(
                    phaseConfig: PhaseConfigurationService,
                    phaserState: PhaserState<IrModuleFragment>,
                    context: JvmBackendContext,
                    input: IrModuleFragment,
                ): IrModuleFragment {
                    topPhase.runBefore(phaseConfig, phaserState, context, input)
                    context.inVerbosePhase = phaseConfig.isVerbose(topPhase)

                    invokePhaseMeasuringTime(profile, topPhase.name) {
                        topPhase.phaseBody(phaseConfig, phaserState, context, input)
                    }

                    topPhase.runAfter(phaseConfig, phaserState, context, input, input)
                    return input
                }
            }

            listOf(NamedCompilerPhase(topPhase.name, topPhase.description, lower = lower))
        }
    }

    return newPhases as MutableList<CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>>
}

class CustomPerFileAggregateLoweringPhase(
    private val filePhases: List<NamedCompilerPhase<JvmBackendContext, IrFile>>,
    private val profile: Boolean,
) : SameTypeCompilerPhase<JvmBackendContext, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<IrModuleFragment>,
        context: JvmBackendContext,
        input: IrModuleFragment,
    ): IrModuleFragment {
        val filePhaserState = phaserState.changePhaserStateType<IrModuleFragment, IrFile>()
        for (filePhase in filePhases) {
            if (phaseConfig.isEnabled(filePhase)) {
                for (irFile in input.files) {
                    filePhase.runBefore(phaseConfig, filePhaserState, context, irFile)
                }
                context.inVerbosePhase = phaseConfig.isVerbose(filePhase)

                invokePhaseMeasuringTime(profile, (filePhase as? NamedCompilerPhase<*, *>)?.name ?: filePhase.javaClass.simpleName) {
                    for (irFile in input.files) {
                        filePhase.phaseBody(phaseConfig, filePhaserState, context, irFile)
                    }
                }

                for (irFile in input.files) {
                    filePhase.runAfter(phaseConfig, filePhaserState, context, irFile, irFile)
                }

                phaserState.alreadyDone.add(filePhase)
                phaserState.phaseCount++
            } else {
                for (irFile in input.files) {
                    filePhase.outputIfNotEnabled(phaseConfig, filePhaserState, context, irFile)
                }
            }
        }

        return input
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, AbstractNamedCompilerPhase<JvmBackendContext, *, *>>> {
        return filePhases.map { startDepth + 1 to it }
    }
}

@OptIn(ExperimentalContracts::class)
private fun <R> invokePhaseMeasuringTime(profile: Boolean, name: String, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val (result, time) = measureTimedValue(block)
    println("Phase $name: ${time.toString(DurationUnit.MILLISECONDS, 2).removeSuffix("ms")}")
    return result
}

private class ConvertIrToBirPhase(name: String, description: String, private val profile: Boolean) :
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

        val birContext: JvmBirBackendContext
        val birModule: BirModuleFragment
        invokePhaseMeasuringTime(profile, "!BIR - convert IR to BIR") {
            birContext = JvmBirBackendContext(
                context,
                input.descriptor,
                compiledBir,
                ir2BirConverter,
                dynamicPropertyManager,
                birPhases,
            )

            birModule = ir2BirConverter.remapElement<BirModuleFragment>(input)
        }

        birModule.countAllDescendants()
        //measureElementDistribution(birModule)

        return BirCompilationBundle(birModule, birContext, input, profile)
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

private class BirCompilationBundle(
    val birModule: BirModuleFragment,
    val backendContext: JvmBirBackendContext,
    val originalIrModuleFragment: IrModuleFragment,
    val profile: Boolean,
)

private object BirLowering : SameTypeCompilerPhase<JvmBackendContext, BirCompilationBundle> {
    override fun invoke(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<BirCompilationBundle>,
        context: JvmBackendContext,
        input: BirCompilationBundle,
    ): BirCompilationBundle {
        val compiledBir = input.backendContext.compiledBir
        val profile = input.profile

        invokePhaseMeasuringTime(profile, "!BIR - applyNewRegisteredIndices") {
            compiledBir.applyNewRegisteredIndices()
        }
        repeat(1) {
            invokePhaseMeasuringTime(profile, "!BIR - baseline tree traversal") {
                input.birModule.countAllDescendants()
            }

            invokePhaseMeasuringTime(profile, "!BIR - reindexAllElements") {
                compiledBir.reindexAllElements()
            }
            //Thread.sleep(100)
        }
        //exitProcess(0)

        for (phase in input.backendContext.loweringPhases) {
            invokePhaseMeasuringTime(profile, "!BIR - ${phase.javaClass.simpleName}") {
                phase(input.birModule)
            }
        }

        return input
    }
}

private class ConvertBirToIrPhase(name: String, description: String) :
    SimpleNamedCompilerPhase<JvmBackendContext, BirCompilationBundle, IrModuleFragment>(name, description) {
    override fun phaseBody(context: JvmBackendContext, input: BirCompilationBundle): IrModuleFragment {
        return input.originalIrModuleFragment
    }

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<BirCompilationBundle>,
        context: JvmBackendContext,
        input: BirCompilationBundle,
    ): IrModuleFragment {
        return input.originalIrModuleFragment
    }
}

private fun BirElement.countAllDescendants(): Int {
    var count = 0
    accept {
        count++
        it.walkIntoChildren()
    }
    return count
}

private val terminateProcessPhase =
    NamedCompilerPhase("Terminate", "Goodbay", lower = object : SameTypeCompilerPhase<JvmBackendContext, IrFile> {
        override fun invoke(
            phaseConfig: PhaseConfigurationService,
            phaserState: PhaserState<IrFile>,
            context: JvmBackendContext,
            input: IrFile,
        ): IrFile {
            exitProcess(0)
        }
    })