package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CheckDeclarationParentsVisitor
import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.utils.DFS
import java.util.Collections.emptySet

internal fun moduleValidationCallback(state: ActionState, module: IrModuleFragment, context: Context) {
    if (!context.config.needCompilerVerification) return

    val validatorConfig = IrValidatorConfig(
        abortOnError = false,
        ensureAllNodesAreDifferent = true,
        checkTypes = true,
        checkDescriptors = false
    )
    try {
        module.accept(IrValidator(context, validatorConfig), null)
        module.accept(CheckDeclarationParentsVisitor, null)
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

internal fun fileValidationCallback(state: ActionState, irFile: IrFile, context: Context) {
    val validatorConfig = IrValidatorConfig(
        abortOnError = false,
        ensureAllNodesAreDifferent = true,
        checkTypes = true,
        checkDescriptors = false
    )
    try {
        irFile.accept(IrValidator(context, validatorConfig), null)
        irFile.accept(CheckDeclarationParentsVisitor, null)
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

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

/**
 * Valid from [createSymbolTablePhase] until [destroySymbolTablePhase].
 */
private var Context.symbolTable: SymbolTable? by Context.nullValue()

internal val createSymbolTablePhase = konanUnitPhase(
        op = {
            this.symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
        },
        name = "CreateSymbolTable",
        description = "Create SymbolTable"
)

internal val objCExportPhase = konanUnitPhase(
        op = {
            objCExport = ObjCExport(this, symbolTable!!)
        },
        name = "ObjCExport",
        description = "Objective-C header generation",
        prerequisite = setOf(createSymbolTablePhase)
)

internal val buildCExportsPhase = konanUnitPhase(
        op = {
            if (this.isNativeLibrary) {
                this.cAdapterGenerator = CAdapterGenerator(this).also {
                    it.buildExports(this.symbolTable!!)
                }
            }
        },
        name = "BuildCExports",
        description = "Build C exports",
        prerequisite = setOf(createSymbolTablePhase)
)

internal val psiToIrPhase = konanUnitPhase(
        op = {
            // Translate AST to high level IR.
            val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER)?:false

            val symbolTable = symbolTable!!

            val translator = Psi2IrTranslator(config.configuration.languageVersionSettings, Psi2IrConfiguration(false))
            val generatorContext = translator.createGeneratorContext(moduleDescriptor, bindingContext, symbolTable)

            val pluginExtensions = IrGenerationExtension.getInstances(config.project)

            val forwardDeclarationsModuleDescriptor = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

            val modulesWithoutDCE = moduleDescriptor.allDependencyModules
                    .filter { !llvmModuleSpecification.isFinal && llvmModuleSpecification.containsModule(it) }

            // Note: using [llvmModuleSpecification] since this phase produces IR for generating single LLVM module.

            val exportedDependencies = (getExportedDependencies() + modulesWithoutDCE).distinct()
            val functionIrClassFactory = BuiltInFictitiousFunctionIrClassFactory(
                    symbolTable, generatorContext.irBuiltIns, reflectionTypes)
            generatorContext.irBuiltIns.functionFactory = functionIrClassFactory
            val stubGenerator = DeclarationStubGenerator(
                    moduleDescriptor, symbolTable,
                    config.configuration.languageVersionSettings
            )
            val symbols = KonanSymbols(this, generatorContext.irBuiltIns, symbolTable, symbolTable.lazyWrapper, functionIrClassFactory)

            val irProviderForCEnumsAndCStructs =
                    IrProviderForCEnumAndCStructStubs(generatorContext, interopBuiltIns, symbols)

            val deserializeFakeOverrides = config.configuration.getBoolean(CommonConfigurationKeys.DESERIALIZE_FAKE_OVERRIDES)

            val linker =
                KonanIrLinker(
                    moduleDescriptor,
                    functionIrClassFactory,
                    this as LoggingContext,
                    generatorContext.irBuiltIns,
                    symbolTable,
                    forwardDeclarationsModuleDescriptor,
                    stubGenerator,
                    irProviderForCEnumsAndCStructs,
                    exportedDependencies,
                    deserializeFakeOverrides
            )

            translator.addPostprocessingStep { module ->
                val pluginContext = IrPluginContextImpl(
                        generatorContext.moduleDescriptor,
                        generatorContext.bindingContext,
                        generatorContext.languageVersionSettings,
                        generatorContext.symbolTable,
                        generatorContext.typeTranslator,
                        generatorContext.irBuiltIns,
                        linker = linker
                )
                pluginExtensions.forEach { extension ->
                    extension.generate(module, pluginContext)
                }
            }

            var dependenciesCount = 0
            while (true) {
                // context.config.librariesWithDependencies could change at each iteration.
                val dependencies = moduleDescriptor.allDependencyModules.filter {
                    config.librariesWithDependencies(moduleDescriptor).contains(it.konanLibrary)
                }

                fun sortDependencies(dependencies: List<ModuleDescriptor>): Collection<ModuleDescriptor> {
                    return DFS.topologicalOrder(dependencies) {
                        it.allDependencyModules
                    }.reversed()
                }

                for (dependency in sortDependencies(dependencies).filter { it != moduleDescriptor }) {
                    val kotlinLibrary = dependency.getCapability(KlibModuleOrigin.CAPABILITY)?.let {
                        (it as? DeserializedKlibModuleOrigin)?.library
                    }
                    linker.deserializeIrModuleHeader(dependency, kotlinLibrary)
                }
                if (dependencies.size == dependenciesCount) break
                dependenciesCount = dependencies.size
            }

            // We need to run `buildAllEnumsAndStructsFrom` before `generateModuleFragment` because it adds references to symbolTable
            // that should be bound.
            modulesWithoutDCE
                    .filter(ModuleDescriptor::isFromInteropLibrary)
                    .forEach(irProviderForCEnumsAndCStructs::referenceAllEnumsAndStructsFrom)

            val irProviders = listOf(linker)

            expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
            val module = translator.generateModuleFragment(
                generatorContext,
                environment.getSourceFiles(),
                irProviders,
                pluginExtensions,
                // TODO: This is a hack to allow platform libs to build in reasonable time.
                // referenceExpectsForUsedActuals() appears to be quadratic in time because of
                // how ExpectedActualResolver is implemented.
                // Need to fix ExpectActualResolver to either cache expects or somehow reduce the member scope searches.
                if (expectActualLinker) expectDescriptorToSymbol else null
            )

            linker.postProcess()

            if (this.stdlibModule in modulesWithoutDCE) {
                functionIrClassFactory.buildAllClasses()
            }

            // Enable lazy IR genration for newly-created symbols inside BE
            stubGenerator.unboundSymbolGeneration = true

            symbolTable.noUnboundLeft("Unbound symbols left after linker")

            module.acceptVoid(ManglerChecker(KonanManglerIr, Ir2DescriptorManglerAdapter(KonanManglerDesc)))
            if (!config.configuration.getBoolean(KonanConfigKeys.DISABLE_FAKE_OVERRIDE_VALIDATOR)) {
                val fakeOverrideChecker = FakeOverrideChecker(KonanManglerIr, KonanManglerDesc)
                linker.modules.values.forEach { fakeOverrideChecker.check(it) }
            }

            irModule = module

            // Note: coupled with [shouldLower] below.
            irModules = linker.modules.filterValues { llvmModuleSpecification.containsModule(it) }

            ir.symbols = symbols

            functionIrClassFactory.module =
                    (listOf(irModule!!) + linker.modules.values)
                            .single { it.descriptor.isNativeStdlib() }
        },
        name = "Psi2Ir",
        description = "Psi to IR conversion",
        prerequisite = setOf(createSymbolTablePhase)
)

// Coupled with [psiToIrPhase] logic above.
internal fun shouldLower(context: Context, declaration: IrDeclaration): Boolean {
    return context.llvmModuleSpecification.containsDeclaration(declaration)
}

internal val destroySymbolTablePhase = konanUnitPhase(
        op = {
            this.symbolTable = null // TODO: invalidate symbolTable itself.
        },
        name = "DestroySymbolTable",
        description = "Destroy SymbolTable",
        prerequisite = setOf(createSymbolTablePhase)
)

// TODO: We copy default value expressions from expects to actuals before IR serialization,
// because the current infrastructure doesn't allow us to get them at deserialization stage.
// That requires some design and implementation work.
internal val copyDefaultValuesToActualPhase = konanUnitPhase(
        op = {
            ExpectToActualDefaultValueCopier(irModule!!).process()
        },
        name = "CopyDefaultValuesToActual",
        description = "Copy default values from expect to actual declarations"
)

internal val serializerPhase = konanUnitPhase(
        op = {
            val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER)?:false

            serializedIr = irModule?.let { ir ->
                KonanIrModuleSerializer(
                    this, ir.irBuiltins, expectDescriptorToSymbol, skipExpects = !expectActualLinker
                ).serializedIrModule(ir)
            }

            val serializer = KlibMetadataMonolithicSerializer(
                this.config.configuration.languageVersionSettings,
                config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
                !expectActualLinker)
            serializedMetadata = serializer.serializeModule(moduleDescriptor)
        },
        name = "Serializer",
        description = "Serialize descriptor tree and inline IR bodies"
)

internal val objectFilesPhase = konanUnitPhase(
        op = { compilerOutput = BitcodeCompiler(this).makeObjectFiles(bitcodeFileName) },
        name = "ObjectFiles",
        description = "Bitcode to object file"
)

internal val linkerPhase = konanUnitPhase(
        op = { Linker(this).link(compilerOutput) },
        name = "Linker",
        description = "Linker"
)

internal val allLoweringsPhase = NamedCompilerPhase(
        name = "IrLowering",
        description = "IR Lowering",
        // TODO: The lowerings before inlinePhase should be aligned with [NativeInlineFunctionResolver.kt]
        lower = removeExpectDeclarationsPhase then
                stripTypeAliasDeclarationsPhase then
                lowerBeforeInlinePhase then
                arrayConstructorPhase then
                lateinitPhase then
                sharedVariablesPhase then
                extractLocalClassesFromInlineBodies then
                inlinePhase then
                provisionalFunctionExpressionPhase then
                lowerAfterInlinePhase then
                performByIrFile(
                        name = "IrLowerByFile",
                        description = "IR Lowering by file",
                        lower = listOf(
                            rangeContainsLoweringPhase,
                            forLoopsPhase,
                            flattenStringConcatenationPhase,
                            foldConstantLoweringPhase,
                            stringConcatenationPhase,
                            enumConstructorsPhase,
                            initializersPhase,
                            localFunctionsPhase,
                            tailrecPhase,
                            defaultParameterExtentPhase,
                            innerClassPhase,
                            dataClassesPhase,
                            ifNullExpressionsFusionPhase,
                            testProcessorPhase,
                            delegationPhase,
                            functionReferencePhase,
                            singleAbstractMethodPhase,
                            builtinOperatorPhase,
                            finallyBlocksPhase,
                            enumClassPhase,
                            interopPhase,
                            varargPhase,
                            compileTimeEvaluatePhase,
                            kotlinNothingValueExceptionPhase,
                            coroutinesPhase,
                            typeOperatorPhase,
                            bridgesPhase,
                            autoboxPhase,
                            returnsInsertionPhase,
                        )
                ),
        actions = setOf(defaultDumper, ::moduleValidationCallback)
)

internal val dependenciesLowerPhase = NamedCompilerPhase(
        name = "LowerLibIR",
        description = "Lower library's IR",
        prerequisite = emptySet(),
        lower = object : CompilerPhase<Context, IrModuleFragment, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
                val files = mutableListOf<IrFile>()
                files += input.files
                input.files.clear()

                // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
                context.librariesWithDependencies
                        .reversed()
                        .forEach {
                            val libModule = context.irModules[it.libraryName]
                                    ?: return@forEach

                            input.files += libModule.files
                            allLoweringsPhase.invoke(phaseConfig, phaserState, context, input)

                            input.files.clear()
                        }

                // Save all files for codegen in reverse topological order.
                // This guarantees that libraries initializers are emitted in correct order.
                context.librariesWithDependencies
                        .forEach {
                            val libModule = context.irModules[it.libraryName]
                                    ?: return@forEach
                            input.files += libModule.files
                        }

                input.files += files

                return input
            }
        })

internal val entryPointPhase = makeCustomPhase<Context, IrModuleFragment>(
        name = "addEntryPoint",
        description = "Add entry point for program",
        prerequisite = emptySet(),
        op = { context, _ ->
            assert(context.config.produce == CompilerOutputKind.PROGRAM)

            val originalFile = context.ir.symbols.entryPoint!!.owner.file
            val originalModule = originalFile.packageFragmentDescriptor.containingDeclaration
            val file = if (context.llvmModuleSpecification.containsModule(originalModule)) {
                originalFile
            } else {
                // `main` function is compiled to other LLVM module.
                // For example, test running support uses `main` defined in stdlib.
                context.irModule!!.addFile(originalFile.fileEntry, originalFile.fqName)
            }

            require(context.llvmModuleSpecification.containsModule(
                    file.packageFragmentDescriptor.containingDeclaration))

            file.addChild(makeEntryPoint(context))
        }
)

internal val bitcodePhase = NamedCompilerPhase(
        name = "Bitcode",
        description = "LLVM Bitcode generation",
        lower = contextLLVMSetupPhase then
                buildDFGPhase then
                devirtualizationPhase then
                dcePhase then
                createLLVMDeclarationsPhase then
                ghaPhase then
                RTTIPhase then
                generateDebugInfoHeaderPhase then
                escapeAnalysisPhase then
                localEscapeAnalysisPhase then
                codegenPhase then
                finalizeDebugInfoPhase then
                cStubsPhase
)

private val backendCodegen = namedUnitPhase(
        name = "Backend codegen",
        description = "Backend code generation",
        lower = takeFromContext<Context, Unit, IrModuleFragment> { it.irModule!! } then
                allLoweringsPhase then // Lower current module first.
                dependenciesLowerPhase then // Then lower all libraries in topological order.
                                            // With that we guarantee that inline functions are unlowered while being inlined.
                entryPointPhase then
                bitcodePhase then
                verifyBitcodePhase then
                printBitcodePhase then
                linkBitcodeDependenciesPhase then
                bitcodeOptimizationPhase then
                unitSink()
)

// Have to hide Context as type parameter in order to expose toplevelPhase outside of this module.
val toplevelPhase: CompilerPhase<*, Unit, Unit> = namedUnitPhase(
        name = "Compiler",
        description = "The whole compilation process",
        lower = frontendPhase then
                createSymbolTablePhase then
                objCExportPhase then
                buildCExportsPhase then
                psiToIrPhase then
                destroySymbolTablePhase then
                copyDefaultValuesToActualPhase then
                serializerPhase then
                namedUnitPhase(
                        name = "Backend",
                        description = "All backend",
                        lower = backendCodegen then
                                produceOutputPhase then
                                disposeLLVMPhase then
                                unitSink()
                ) then
                objectFilesPhase then
                linkerPhase
)

internal fun PhaseConfig.disableIf(phase: AnyNamedPhase, condition: Boolean) {
    if (condition) disable(phase)
}

internal fun PhaseConfig.disableUnless(phase: AnyNamedPhase, condition: Boolean) {
    if (!condition) disable(phase)
}

internal fun PhaseConfig.konanPhasesConfig(config: KonanConfig) {
    with(config.configuration) {
        disable(compileTimeEvaluatePhase)
        disable(escapeAnalysisPhase)

        // Don't serialize anything to a final executable.
        disableUnless(serializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        disableIf(dependenciesLowerPhase, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(entryPointPhase, config.produce == CompilerOutputKind.PROGRAM)
        disableIf(bitcodePhase, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(bitcodeOptimizationPhase, config.produce.involvesLinkStage)
        disableUnless(linkBitcodeDependenciesPhase, config.produce.involvesLinkStage)
        disableUnless(objectFilesPhase, config.produce.involvesLinkStage)
        disableUnless(linkerPhase, config.produce.involvesLinkStage)
        disableIf(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE)
        disableUnless(buildDFGPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(devirtualizationPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(localEscapeAnalysisPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(dcePhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(ghaPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(verifyBitcodePhase, config.needCompilerVerification || getBoolean(KonanConfigKeys.VERIFY_BITCODE))

        val isDescriptorsOnlyLibrary = config.metadataKlib == true
        disableIf(psiToIrPhase, isDescriptorsOnlyLibrary)
        disableIf(destroySymbolTablePhase, isDescriptorsOnlyLibrary)
        disableIf(copyDefaultValuesToActualPhase, isDescriptorsOnlyLibrary)
        disableIf(backendCodegen, isDescriptorsOnlyLibrary)
    }
}
