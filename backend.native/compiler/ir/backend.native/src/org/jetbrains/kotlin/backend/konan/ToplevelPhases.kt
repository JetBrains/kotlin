package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CheckDeclarationParentsVisitor
import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
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
        op = { this.psiToIr(symbolTable!!) },
        name = "Psi2Ir",
        description = "Psi to IR conversion and klib linkage",
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

internal val exportInternalAbiPhase = makeKonanModuleOpPhase(
        name = "exportInternalAbi",
        description = "Add accessors to private entities",
        prerequisite = emptySet(),
        op = { context, module ->
            val visitor = object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)
                    if (declaration.isCompanion) {
                        val function = context.irFactory.buildFun {
                            name = InternalAbi.getCompanionObjectAccessorName(declaration)
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            returnType = declaration.defaultType
                        }
                        context.createIrBuilder(function.symbol).apply {
                            function.body = irBlockBody {
                                +irReturn(irGetObjectValue(declaration.defaultType, declaration.symbol))
                            }
                        }
                        context.internalAbi.declare(function, declaration.module)
                    }

                }
            }
            module.acceptChildrenVoid(visitor)
        }
)

internal val useInternalAbiPhase = makeKonanModuleOpPhase(
        name = "useInternalAbi",
        description = "Use internal ABI functions to access private entities",
        prerequisite = emptySet(),
        op = { context, module ->
            val accessors = mutableMapOf<IrClass, IrSimpleFunction>()
            val transformer = object : IrElementTransformerVoid() {
                override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                    val irClass = expression.symbol.owner
                    if (!irClass.isCompanion || context.llvmModuleSpecification.containsDeclaration(irClass)) {
                        return expression
                    }
                    val accessor = accessors.getOrPut(irClass) {
                        context.irFactory.buildFun {
                            name = InternalAbi.getCompanionObjectAccessorName(irClass)
                            returnType = irClass.defaultType
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            isExternal = true
                        }.also {
                            context.internalAbi.reference(it, irClass.module)
                        }
                    }
                    return IrCallImpl(expression.startOffset, expression.endOffset, expression.type, accessor.symbol)
                }
            }
            module.transformChildrenVoid(transformer)
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
                exportInternalAbiPhase then
                useInternalAbiPhase then
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
        disable(localEscapeAnalysisPhase)

        // Don't serialize anything to a final executable.
        disableUnless(serializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        disableIf(dependenciesLowerPhase, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(entryPointPhase, config.produce == CompilerOutputKind.PROGRAM)
        disableUnless(exportInternalAbiPhase, config.produce.isCache)
        disableIf(bitcodePhase, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(bitcodeOptimizationPhase, config.produce.involvesLinkStage)
        disableUnless(linkBitcodeDependenciesPhase, config.produce.involvesLinkStage)
        disableUnless(objectFilesPhase, config.produce.involvesLinkStage)
        disableUnless(linkerPhase, config.produce.involvesLinkStage)
        disableIf(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE)
        disableUnless(buildDFGPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(devirtualizationPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(escapeAnalysisPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
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
