package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import java.util.Collections.emptySet

internal fun konanUnitPhase(
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet(),
        op: Context.() -> Unit
) = namedOpUnitPhase(name, description, prerequisite, op)

internal val frontendPhase = konanUnitPhase(
        op = {
            val environment = environment
            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector,
                    environment.configuration.languageVersionSettings)

            // Build AST and binding info.
            analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
                TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), this)
            }
            if (analyzerWithCompilerReport.hasErrors()) {
                throw KonanCompilationException()
            }
            moduleDescriptor = analyzerWithCompilerReport.analysisResult.moduleDescriptor
            bindingContext = analyzerWithCompilerReport.analysisResult.bindingContext
        },
        name = "Frontend",
        description = "Frontend builds AST"
)

internal val psiToIrPhase = konanUnitPhase(
        op = {
            // Translate AST to high level IR.
            val translator = Psi2IrTranslator(config.configuration.languageVersionSettings,
                    Psi2IrConfiguration(false))
            val generatorContext = translator.createGeneratorContext(moduleDescriptor, bindingContext)
            @Suppress("DEPRECATION")
            psi2IrGeneratorContext = generatorContext

            val forwardDeclarationsModuleDescriptor = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

            val deserializer = KonanIrModuleDeserializer(
                    moduleDescriptor,
                    this as LoggingContext,
                    generatorContext.irBuiltIns,
                    generatorContext.symbolTable,
                    forwardDeclarationsModuleDescriptor
            )

            val irModules = moduleDescriptor.allDependencyModules.map {
                val library  = it.konanLibrary
                if (library == null) {
                    return@map null
                }
                library.irHeader?.let { header -> deserializer.deserializeIrModule(it, header) }
            }.filterNotNull()

            val symbols = KonanSymbols(this, generatorContext.symbolTable, generatorContext.symbolTable.lazyWrapper)
            val module = translator.generateModuleFragment(generatorContext, environment.getSourceFiles(), deserializer)

            irModules.forEach {
                it.patchDeclarationParents()
            }

            irModule = module
            ir.symbols = symbols

//        validateIrModule(this, module)
        },
        name = "Psi2Ir",
        description = "Psi to IR conversion"
)

internal val irGeneratorPluginsPhase = konanUnitPhase(
        op = {
            val extensions = IrGenerationExtension.getInstances(config.project)
            extensions.forEach { extension ->
                irModule!!.files.forEach {
                    irFile -> extension.generate(irFile, this, bindingContext)
                }
            }
        },
        name = "IrGeneratorPlugins",
        description = "Plugged-in ir generators"
)

internal val genSyntheticFieldsPhase = konanUnitPhase(
        op = { markBackingFields(this) },
        name = "GenSyntheticFields",
        description = "Generate synthetic fields"
)

// TODO: We copy default value expressions from expects to actuals before IR serialization,
// because the current infrastructure doesn't allow us to get them at deserialization stage.
// That requires some design and implementation work.
internal val copyDefaultValuesToActualPhase = konanUnitPhase(
        op = {
            irModule!!.files.forEach(ExpectToActualDefaultValueCopier(this)::lower)
        },
        name = "CopyDefaultValuesToActual",
        description = "Copy default values from expect to actual declarations"
)

internal val patchDeclarationParents0Phase = konanUnitPhase(
        op = { irModule!!.patchDeclarationParents() }, // why do we need it?
        name = "PatchDeclarationParents0",
        description = "Patch declaration parents"
)

internal val serializerPhase = konanUnitPhase(
        op = {
            val declarationTable = DeclarationTable(irModule!!.irBuiltins, DescriptorTable())
            val serializedIr = IrModuleSerializer(
                    this, declarationTable, bodiesOnlyForInlines = config.isInteropStubs).serializedIrModule(irModule!!)
            val serializer = KonanSerializationUtil(this, config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!, declarationTable)
            serializedLinkData =
                    serializer.serializeModule(moduleDescriptor, /*if (!config.isInteropStubs) serializedIr else null*/ serializedIr)
        },
        name = "Serializer",
        description = "Serialize descriptor tree and inline IR bodies",
        prerequisite = setOf(genSyntheticFieldsPhase)
)

internal val setUpLinkStagePhase = konanUnitPhase(
        op =  { linkStage = LinkStage(this) },
        name = "SetUpLinkStage",
        description = "Set up link stage"
)

internal val objectFilesPhase = konanUnitPhase(
        op = { linkStage.makeObjectFiles() },
        name = "ObjectFiles",
        description = "Bitcode to object file"
)

internal val linkerPhase = konanUnitPhase(
        op = { linkStage.linkStage() },
        name = "Linker",
        description = "Linker"
)

internal val linkPhase = namedUnitPhase(
        name = "Link",
        description = "Link stage",
        lower = setUpLinkStagePhase then
                objectFilesPhase then
                linkerPhase
)

internal val toplevelPhase = namedUnitPhase(
        name = "Compiler",
        description = "The whole compilation process",
        lower = frontendPhase then
                psiToIrPhase then
                irGeneratorPluginsPhase then
                genSyntheticFieldsPhase then
                copyDefaultValuesToActualPhase then
                patchDeclarationParents0Phase then
                serializerPhase then
                namedUnitPhase(
                        name = "Backend",
                        description = "All backend",
                        lower = takeFromContext<Context, Unit, IrModuleFragment> { it.irModule!! } then
                                namedIrModulePhase(
                                        name = "IrLowering",
                                        description = "IR Lowering",
                                        lower = removeExpectDeclarationsPhase then
                                                lowerBeforeInlinePhase then
                                                inlinePhase then
                                                lowerAfterInlinePhase then
                                                interopPart1Phase then
                                                patchDeclarationParents1Phase then
                                                performByIrFile(
                                                        name = "IrLowerByFile",
                                                        description = "IR Lowering by file",
                                                        lower = lateinitPhase then
                                                                stringConcatenationPhase then
                                                                enumConstructorsPhase then
                                                                initializersPhase then
                                                                sharedVariablesPhase then
                                                                localFunctionsPhase then
                                                                tailrecPhase then
                                                                defaultParameterExtentPhase then
                                                                innerClassPhase then
                                                                forLoopsPhase then
                                                                dataClassesPhase then
                                                                builtinOperatorPhase then
                                                                finallyBlocksPhase then
                                                                testProcessorPhase then
                                                                enumClassPhase then
                                                                delegationPhase then
                                                                callableReferencePhase then
                                                                interopPart2Phase then
                                                                varargPhase then
                                                                compileTimeEvaluatePhase then
                                                                coroutinesPhase then
                                                                typeOperatorPhase then
                                                                bridgesPhase then
                                                                autoboxPhase then
                                                                returnsInsertionPhase
                                                ) then
                                                checkDeclarationParentsPhase then
//                                                validateIrModulePhase then // Temporarily disabled until moving to new IR finished.
                                                moduleIndexForCodegenPhase
                                ) then
                                namedIrModulePhase(
                                        name = "Bitcode",
                                        description = "LLVM BitCode Generation",
                                        lower = contextLLVMSetupPhase then
                                                RTTIPhase then
                                                generateDebugInfoHeaderPhase then
                                                buildDFGPhase then
                                                deserializeDFGPhase then
                                                devirtualizationPhase then
                                                escapeAnalysisPhase then
                                                serializeDFGPhase then
                                                codegenPhase then
                                                finalizeDebugInfoPhase then
                                                cStubsPhase then
                                                bitcodeLinkerPhase
                                ) then
                                verifyBitcodePhase then
                                printBitcodePhase
                                then
                                unitSink()
                ) then
                linkPhase
)

internal fun PhaseConfig.konanPhasesConfig(config: KonanConfig) {
    with(config.configuration) {
        disable(compileTimeEvaluatePhase)
        disable(buildDFGPhase)
        disable(deserializeDFGPhase)
        disable(devirtualizationPhase)
        disable(escapeAnalysisPhase)
        disable(serializeDFGPhase)

        // Don't serialize anything to a final executable.
        switch(serializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        switch(linkPhase, config.produce.isNativeBinary)
        switch(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE)
    }
}