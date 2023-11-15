/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.lower.*
import org.jetbrains.kotlin.bir.declarations.BirExternalPackageFragment
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.util.Bir2IrConverter
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.bir.util.countAllElementsInTree
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformFlat
import java.util.IdentityHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

val allBirPhases = listOf<Pair<(JvmBirBackendContext) -> BirLoweringPhase, List<String>>>(
    ::BirJvmNameLowering to listOf(),
    ::BirJvmStaticInObjectLowering to listOf("JvmStaticInObject"),
    ::BirRepeatedAnnotationLowering to listOf("RepeatedAnnotation"),
    ::BirTypeAliasAnnotationMethodsLowering to listOf("TypeAliasAnnotationMethodsLowering"),
    ::BirProvisionalFunctionExpressionLowering to listOf("FunctionExpression"),
    ::BirJvmOverloadsAnnotationLowering to listOf("JvmOverloadsAnnotation"),
    ::BirMainMethodGenerationLowering to listOf("MainMethodGeneration"),
    ::BirPolymorphicSignatureLowering to listOf("PolymorphicSignature"),
    ::BirVarargLowering to listOf("VarargLowering"),
    ::BirJvmLateinitLowering to listOf("JvmLateinitLowering"),
    ::BirJvmInventNamesForLocalClassesLowering to listOf("InventNamesForLocalClasses"),
    ::BirInlineCallableReferenceToLambdaLowering to listOf("InlineCallableReferenceToLambdaPhase"),
    ::BirDirectInvokeLowering to listOf("DirectInvokes"),
    //::BirAnnotationLowering to listOf("Annotation"),
)

private val excludedPhases = setOf<String>(
    //"Bir2Ir",
    //"Terminate",

    // This phase removes annotation constructors, but they are still being used,
    // which causes an exception in BIR. It works in IR because removed constructors
    // still have their parent property set.
    "Annotation",
    // This phase is not implemented, as it is hardly ever relevant.
    "AnnotationImplementation",
)

fun lowerWithBir(
    phases: SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment>,
    context: JvmBackendContext,
    irModuleFragment: IrModuleFragment,
) {
    val newPhases = reconstructPhases(phases, context.phaseConfig.needProfiling)

    val birPhases = listOf<AbstractNamedCompilerPhase<JvmBackendContext, *, *>>(
        ConvertIrToBirPhase(
            "Ir2Bir",
            "Convert IR to BIR",
            context.phaseConfig.needProfiling,
        ),
        NamedCompilerPhase(
            "Lower Bir",
            "Experimental phase to test alternative IR architecture",
            lower = BirLowering,
        ),
        ConvertBirToIrPhase(
            "Bir2Ir",
            "Convert lowered BIR back to IR",
        ),
    )
    newPhases.addAll(
        newPhases.indexOfFirst { (it as AnyNamedPhase).name == "FileClass" } + 1,
        birPhases as List<NamedCompilerPhase<JvmBackendContext, IrModuleFragment>>
    )

    val allExcludedPhases = excludedPhases// + allBirPhases.flatMap { it.second }

    val compoundPhase = newPhases.reduce { result, phase -> result then phase }
    val phaseConfig = PhaseConfigBuilder(compoundPhase).apply {
        enabled += compoundPhase.toPhaseMap().values.filter { it.name !in allExcludedPhases }.toSet()
        verbose += context.phaseConfig.verbose
        toDumpStateBefore += context.phaseConfig.toDumpStateBefore
        toDumpStateAfter += context.phaseConfig.toDumpStateAfter
        dumpToDirectory = context.phaseConfig.dumpToDirectory
        dumpOnlyFqName = context.phaseConfig.dumpOnlyFqName
        checkConditions = context.phaseConfig.checkConditions
        checkStickyConditions = context.phaseConfig.checkStickyConditions
    }.build()

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
        .toMutableList() as MutableList<AbstractNamedCompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>>

    newPhases.transformFlat { topPhase ->
        if (topPhase.name == "PerformByIrFile") {
            val filePhases = topPhase.getNamedSubphases()
                .filter { it.first == 1 }
                .map { it.second as AbstractNamedCompilerPhase<JvmBackendContext, IrFile, IrFile> }
                .toMutableList()

            filePhases.add(
                filePhases.indexOfFirst { (it as AnyNamedPhase).name == "DirectInvokes" } + 1,
                terminateProcessPhase as AbstractNamedCompilerPhase<JvmBackendContext, IrFile, IrFile>
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
                    dumpOriginalIrPhase(context, input, topPhase)
                    return input
                }
            }

            listOf(NamedCompilerPhase(topPhase.name, topPhase.description, lower = lower))
        }
    }

    return newPhases as MutableList<CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>>
}

class CustomPerFileAggregateLoweringPhase(
    private val filePhases: List<AbstractNamedCompilerPhase<JvmBackendContext, IrFile, IrFile>>,
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

                invokePhaseMeasuringTime(
                    profile,
                    (filePhase as? AbstractNamedCompilerPhase<*, *, *>)?.name ?: filePhase.javaClass.simpleName
                ) {
                    for (irFile in input.files) {
                        filePhase.phaseBody(phaseConfig, filePhaserState, context, irFile)
                    }
                }

                for (irFile in input.files) {
                    filePhase.runAfter(phaseConfig, filePhaserState, context, irFile, irFile)
                }

                phaserState.alreadyDone.add(filePhase)
                phaserState.phaseCount++

                dumpOriginalIrPhase(context, input, filePhase)
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

        val externalModulesBir = BirForest()
        val compiledBir = BirForest()

        val externalIr2BirElements = IdentityHashMap<BirElement, IrSymbol>()
        val ir2BirConverter = Ir2BirConverter(dynamicPropertyManager)
        ir2BirConverter.copyAncestorsForOrphanedElements = true
        ir2BirConverter.appendElementAsForestRoot = { old, new ->
            if (old is IrSymbolOwner) {
                externalIr2BirElements[new] = old.symbol
            }

            when {
                old === input -> compiledBir
                new is BirModuleFragment || new is BirExternalPackageFragment || new is BirLazyElementBase -> externalModulesBir
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
                externalModulesBir,
                ir2BirConverter,
                dynamicPropertyManager,
                allBirPhases.map { it.first },
            )

            birModule = ir2BirConverter.remapElement<BirModuleFragment>(input)
        }

        val size = birModule.countAllElementsInTree()

        return BirCompilationBundle(
            birModule,
            birContext,
            input,
            externalIr2BirElements,
            dynamicPropertyManager,
            size,
            profile
        )
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

class BirCompilationBundle(
    val birModule: BirModuleFragment,
    val backendContext: JvmBirBackendContext,
    val irModuleFragment: IrModuleFragment,
    val externalIr2BirElements: Map<BirElement, IrSymbol>,
    val dynamicPropertyManager: BirElementDynamicPropertyManager,
    val estimatedIrTreeSize: Int,
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
        val externalBir = input.backendContext.externalModulesBir
        val profile = input.profile

        invokePhaseMeasuringTime(profile, "!BIR - applyNewRegisteredIndices") {
            compiledBir.applyNewRegisteredIndices()
            externalBir.applyNewRegisteredIndices()
        }
        repeat(1) {
            invokePhaseMeasuringTime(profile, "!BIR - baseline tree traversal") {
                input.birModule.countAllElementsInTree()
            }

            invokePhaseMeasuringTime(profile, "!BIR - index compiled BIR") {
                compiledBir.reindexAllElements()
            }
            invokePhaseMeasuringTime(profile, "!BIR - index external BIR") {
                externalBir.reindexAllElements()
            }
            //Thread.sleep(100)
        }
        //exitProcess(0)

        for (phase in input.backendContext.loweringPhases) {
            val phaseName = phase.javaClass.simpleName
            invokePhaseMeasuringTime(profile, "!BIR - $phaseName") {
                phase(input.birModule)
            }

            dumpBirPhase(context, input, phase)
        }

        return input
    }
}

private class ConvertBirToIrPhase(name: String, description: String) :
    SimpleNamedCompilerPhase<JvmBackendContext, BirCompilationBundle, IrModuleFragment>(name, description) {
    override fun phaseBody(context: JvmBackendContext, input: BirCompilationBundle): IrModuleFragment {
        val compiledBir = input.birModule.getContainingForest()!!
        val bir2IrConverter = Bir2IrConverter(
            input.dynamicPropertyManager,
            input.externalIr2BirElements,
            context.irBuiltIns,
            compiledBir,
            input.estimatedIrTreeSize
        )
        val newIrModule: IrModuleFragment
        invokePhaseMeasuringTime(input.profile, "!BIR - convert IR to BIR") {
            newIrModule = bir2IrConverter.remapElement<IrModuleFragment>(input.birModule)
            newIrModule.patchDeclarationParents()
        }
        return newIrModule
    }

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<BirCompilationBundle>,
        context: JvmBackendContext,
        input: BirCompilationBundle,
    ): IrModuleFragment {
        return input.irModuleFragment
    }
}

private val terminateProcessPhase = createSimpleNamedCompilerPhase<LoggingContext, IrFile, IrFile>(
    name = "Terminate",
    description = "Goodbay",
    outputIfNotEnabled = { _, _, _, input -> input },
    op = { _, _ -> exitProcess(0) }
)