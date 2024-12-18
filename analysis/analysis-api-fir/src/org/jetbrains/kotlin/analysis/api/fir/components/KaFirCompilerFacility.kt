/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.components.KaCompilerFacility.Companion.CODE_FRAGMENT_CLASS_NAME
import org.jetbrains.kotlin.analysis.api.components.KaCompilerFacility.Companion.CODE_FRAGMENT_METHOD_NAME
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaBaseCompiledFileForOutputFile
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedId
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticMarker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.CodeFragmentConversionData
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.referencedMemberSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedValueParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.*

internal class KaFirCompilerFacility(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaCompilerFacility, KaFirSessionComponent {

    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult = withValidityAssertion {
        try {
            val effectiveFile = substituteTargetFile(file, target) ?: file
            return compileUnsafe(effectiveFile, configuration, target, allowedErrorFilter)
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            throw KaCodeCompilationException(e)
        }
    }

    private fun substituteTargetFile(file: KtFile, target: KaCompilerTarget): KtFile? {
        val fileModule = firResolveSession.getModule(file)

        if (fileModule.targetPlatform.isCommon() && target is KaCompilerTarget.Jvm) {
            val contextModule = when (fileModule) {
                is KaDanglingFileModule -> fileModule.contextModule
                else -> fileModule
            }

            checkWithAttachment(contextModule !is KaDanglingFileModule, { "Nested common dangling file compilation is not supported" }) {
                withPsiEntry("file", file)
            }

            val jvmImplementingModule = KotlinProjectStructureProvider.getInstance(project)
                .getImplementingModules(contextModule)
                .find { it.targetPlatform.isJvm() }

            checkWithAttachment(jvmImplementingModule != null, { "Cannot compile a common source without a JVM counterpart" }) {
                withPsiEntry("file", file)
            }

            val newName = file.name
            val newText = file.text

            if (file is KtCodeFragment) {
                val fileCopy = when (file) {
                    is KtExpressionCodeFragment -> KtExpressionCodeFragment(project, newName, newText, file.importsToString(), file.context)
                    is KtBlockCodeFragment -> KtBlockCodeFragment(project, newName, newText, file.importsToString(), file.context)
                    is KtTypeCodeFragment -> KtTypeCodeFragment(project, newName, newText, file.context)
                    else -> error("Unsupported code fragment type: " + file.javaClass.name)
                }

                fileCopy.refinedContextModule = jvmImplementingModule
                return fileCopy
            }

            val fileCopy = KtPsiFactory(project).createFile(newName, newText)
            fileCopy.contextModule = jvmImplementingModule
            return fileCopy
        }

        return null
    }

    private fun compileUnsafe(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult {
        val syntaxErrors = SyntaxErrorReportingVisitor(analysisSession.firSession) { it.asKtDiagnostic() }
            .also(file::accept).diagnostics

        if (syntaxErrors.isNotEmpty()) {
            return KaCompilationResult.Failure(syntaxErrors)
        }

        val mainFirFile = getFullyResolvedFirFile(file)

        val frontendDiagnostics = file.collectDiagnosticsForFile(firResolveSession, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        val frontendErrors = computeErrors(frontendDiagnostics, allowedErrorFilter)

        if (frontendErrors.isNotEmpty()) {
            return KaCompilationResult.Failure(frontendErrors)
        }

        val codeFragmentMappings = runIf(file is KtCodeFragment) {
            computeCodeFragmentMappings(file, mainFirFile, firResolveSession, configuration)
        }

        val compilationPeerData = CompilationPeerCollector.process(mainFirFile)

        val filesToCompile = buildList {
            // Since the order of dependency files matters, we have to use "List" here. Otherwise, we will meet a case
            // that it has a missing "inline function" when filling inline functions as a part of the JVM bytecode-gen.
            val dependencyFiles = buildList {
                addAll(compilationPeerData.filesToCompile)

                val filesAsSet = compilationPeerData.filesToCompile.toHashSet()
                codeFragmentMappings?.capturedFiles?.forEach { if (it !in filesAsSet) add(it) }

                // The main file needs to be the last so caches for the context declarations are populated in FIR-to-IR.
                remove(file)
            }

            addAll(dependencyFiles)
            add(file)
        }

        // Files in the code fragment context module are compiled together with the code fragment itself.
        val targetModules = computeTargetModules(mainFirFile.llFirModuleData.ktModule)
        val (targetFiles, dependencyFiles) = filesToCompile.partition { firResolveSession.getModule(it) in targetModules }
        require(targetFiles.isNotEmpty())

        val jvmIrDeserializer = JvmIrDeserializerImpl()
        val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector)

        val inlineFunDependencyBytecode = mutableMapOf<String, ByteArray>()
        for (dependencyFile in dependencyFiles) {
            val effectiveDependencyFile = substituteTargetFile(dependencyFile, target) ?: dependencyFile
            var compileResult: KaCompilationResult? = null
            runFir2IrForDependency(
                effectiveDependencyFile, configuration, jvmIrDeserializer, diagnosticReporter
            ) { fir2IrResult, dependencyConfiguration ->
                val codegenFactory = createJvmIrCodegenFactory(
                    configuration = dependencyConfiguration,
                    isCodeFragment = effectiveDependencyFile is KtCodeFragment,
                    irModuleFragment = fir2IrResult.irModuleFragment
                )

                val generateClassFilter = SingleFileGenerateClassFilter(
                    files = listOf(effectiveDependencyFile),
                    inlinedClasses = compilationPeerData.inlinedClasses
                )

                compileResult = runJvmIrCodeGen(
                    fir2IrResult = fir2IrResult,
                    configuration = dependencyConfiguration,
                    target = target,
                    codeFragmentMappings = null,
                    codegenFactory = codegenFactory,
                    generateClassFilter = generateClassFilter,
                    diagnosticReporter = diagnosticReporter,
                    jvmGeneratorExtensions = JvmFir2IrExtensions(dependencyConfiguration, jvmIrDeserializer),
                    allowedErrorFilter = allowedErrorFilter,
                ) { generationState ->
                    inlineFunDependencyBytecode.forEach { (className, compileResult) ->
                        generationState.inlineCache.classBytes.put(className, compileResult)
                    }
                }
            }
            when (compileResult) {
                is KaCompilationResult.Success -> {
                    val artifact = compileResult as KaCompilationResult.Success
                    for (compiledFile in artifact.output) {
                        val path = compiledFile.path

                        // `GenerationState.inlineCache` uses the path to class file without ".class" as a key. For example,
                        //  - The key for `Foo` class in `com.example.foo` package is `com/example/foo/Foo`.
                        //  - The key for companion object of `Foo` in `com.example.foo` package is `com/example/foo/Foo$Companion`.
                        //  - The key for an inner class `Inner` of `Foo` in `com.example.foo` package is `com/example/foo/Foo$Inner`.
                        if (!path.endsWith(".class")) continue
                        val className = path.substringBeforeLast(".class")

                        inlineFunDependencyBytecode[className] = compiledFile.content
                    }
                }
                is KaCompilationResult.Failure -> return compileResult!!
                null -> continue
            }
        }

        val targetConfiguration = configuration
            .copy()
            .apply {
                put(CommonConfigurationKeys.USE_FIR, true)
            }

        val jvmGeneratorExtensions = JvmFir2IrExtensions(targetConfiguration, jvmIrDeserializer)
        val targetFir2IrExtensions = CompilerFacilityFir2IrExtensions(
            jvmGeneratorExtensions,
            codeFragmentMappings?.injectedValueProvider
        )

        val targetSession = mainFirFile.llFirSession
        val targetFirFiles = targetFiles.map(::getFullyResolvedFirFile)
        val targetFir2IrResult = runFir2Ir(
            targetSession, targetFirFiles, targetFir2IrExtensions, diagnosticReporter, targetConfiguration,
            /**
             * IR for code fragment is not fully correct until `patchCodeFragmentIr` is over.
             * Because of that we run IR plugins manually after patching and don't pass any extension to fir2ir conversion in `runFir2Ir` method
             */
            irGeneratorExtensions = emptyList()
        )

        patchCodeFragmentIr(targetFir2IrResult)

        ProgressManager.checkCanceled()
        targetFir2IrResult.pluginContext.applyIrGenerationExtensions(
            targetFir2IrResult.components.configuration,
            targetFir2IrResult.irModuleFragment,
            getIrGenerationExtensions(targetModules),
        )
        val codegenFactory = createJvmIrCodegenFactory(targetConfiguration, file is KtCodeFragment, targetFir2IrResult.irModuleFragment)

        return runJvmIrCodeGen(
            targetFir2IrResult,
            targetConfiguration,
            target,
            codeFragmentMappings,
            codegenFactory,
            SingleFileGenerateClassFilter(targetFiles, compilationPeerData.inlinedClasses),
            diagnosticReporter,
            jvmGeneratorExtensions,
            allowedErrorFilter,
        ) { generationState ->
            inlineFunDependencyBytecode.forEach { (className, compileResult) ->
                generationState.inlineCache.classBytes.put(className, compileResult)
            }
        }
    }

    private fun runJvmIrCodeGen(
        fir2IrResult: Fir2IrActualizedResult,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        codeFragmentMappings: CodeFragmentMappings?,
        codegenFactory: JvmIrCodegenFactory,
        generateClassFilter: SingleFileGenerateClassFilter,
        diagnosticReporter: PendingDiagnosticsCollectorWithSuppress,
        jvmGeneratorExtensions: JvmGeneratorExtensions,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
        fillInlineCache: (GenerationState) -> Unit,
    ): KaCompilationResult {
        val generationState = GenerationState(
            project,
            fir2IrResult.irModuleFragment.descriptor,
            configuration,
            target.classBuilderFactory,
            generateDeclaredClassFilter = generateClassFilter,
            diagnosticReporter = diagnosticReporter,
        )

        fillInlineCache(generationState)

        ProgressManager.checkCanceled()

        codegenFactory.generateModuleInFrontendIRMode(
            generationState,
            fir2IrResult.irModuleFragment,
            fir2IrResult.symbolTable,
            fir2IrResult.components.irProviders,
            CompilerFacilityJvmGeneratorExtensions(jvmGeneratorExtensions),
            FirJvmBackendExtension(fir2IrResult.components, null),
            fir2IrResult.pluginContext
        )

        CodegenFactory.doCheckCancelled(generationState)
        generationState.factory.done()

        val outputFiles = generationState.factory.asList().map(::KaBaseCompiledFileForOutputFile)
        val capturedValues = buildList {
            if (codeFragmentMappings != null) {
                addAll(codeFragmentMappings.capturedValues)
            }
            for ((_, _, descriptor) in generationState.newFragmentCaptureParameters) {
                if (descriptor is IrBasedDeclarationDescriptor<*>) {
                    addIfNotNull(computeAdditionalCodeFragmentMapping(descriptor))
                }
            }
        }

        return KaCompilationResult.Success(outputFiles, capturedValues)
    }

    private fun getIrGenerationExtensions(modules: List<KaModule>): List<IrGenerationExtension> = buildList {
        modules.forEach { module ->
            val sourceModule = module as? KaSourceModule ?: return@forEach
            KotlinCompilerPluginsProvider.getInstance(project)?.getRegisteredExtensions(sourceModule, IrGenerationExtension)
                ?.let { addAll(it) }
        }
        addAll(IrGenerationExtension.getInstances(project))
    }

    private fun computeTargetModules(module: KaModule): List<KaModule> {
        return when (module) {
            is KaDanglingFileModule -> buildList {
                val contextModule = module.contextModule
                add(contextModule)

                val file = module.file
                if (file is KtCodeFragment) {
                    val contextElement = file.context
                    if (contextElement != null) {
                        val contextElementModule = firResolveSession.getModule(contextElement)
                        if (contextElementModule != contextModule) {
                            add(contextElementModule)
                        }
                    }
                }

                add(module)
            }
            else -> listOf(module)
        }
    }

    private fun runFir2IrForDependency(
        dependencyFile: KtFile,
        configuration: CompilerConfiguration,
        jvmIrDeserializer: JvmIrDeserializerImpl,
        diagnosticReporter: PendingDiagnosticsCollectorWithSuppress,
        handleFir2IrResult: ((Fir2IrActualizedResult, CompilerConfiguration) -> Unit)? = null,
    ) {
        val dependencyFirFile = getFullyResolvedFirFile(dependencyFile)
        val dependencySession = dependencyFirFile.llFirSession
        val dependencyConfiguration = configuration.copy().apply {
            put(CommonConfigurationKeys.USE_FIR, true)
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, dependencySession.languageVersionSettings)
        }

        val dependencyFir2IrExtensions = JvmFir2IrExtensions(dependencyConfiguration, jvmIrDeserializer)
        val fir2IrResult = runFir2Ir(
            dependencySession,
            listOf(dependencyFirFile),
            dependencyFir2IrExtensions,
            diagnosticReporter,
            dependencyConfiguration,
            getIrGenerationExtensions(listOf(dependencyFirFile.llFirModuleData.ktModule))
        )

        if (handleFir2IrResult != null) {
            handleFir2IrResult(fir2IrResult, dependencyConfiguration)
        }
    }

    private fun runFir2Ir(
        session: LLFirSession,
        firFiles: List<FirFile>,
        fir2IrExtensions: Fir2IrExtensions,
        diagnosticReporter: BaseDiagnosticsCollector,
        effectiveConfiguration: CompilerConfiguration,
        irGeneratorExtensions: List<IrGenerationExtension>
    ): Fir2IrActualizedResult {
        val fir2IrConfiguration =
            Fir2IrConfiguration.forAnalysisApi(effectiveConfiguration, session.languageVersionSettings, diagnosticReporter)
        val firResult = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, session.getScopeSession(), firFiles)))
        val singleOutput = firResult.outputs.size == 1
        check(singleOutput) { "Single output invariant is used in the lambda below" }

        return firResult.convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            irGeneratorExtensions,
            JvmIrMangler,
            FirJvmVisibilityConverter,
            DefaultBuiltIns.Instance,
            ::JvmIrTypeSystemContext,
            JvmIrSpecialAnnotationSymbolProvider,
            extraActualDeclarationExtractorsInitializer = {
                error(
                    "extraActualDeclarationExtractorsInitializer should never be called, because outputs is a list of a single element. " +
                            "Output is single ($singleOutput) => " +
                            "dependentIrFragments will always be empty => " +
                            "IrActualizer will never be called => " +
                            "extraActualDeclarationExtractorsInitializer will never be called"
                )
            },
        )
    }

    private fun patchCodeFragmentIr(fir2IrResult: Fir2IrActualizedResult) {
        fun isCodeFragmentFile(irFile: IrFile): Boolean {
            val file = (irFile.metadata as? FirMetadataSource.File)?.fir
            return file?.psi is KtCodeFragment
        }

        val (irCodeFragmentFiles, irOrdinaryFiles) = fir2IrResult.irModuleFragment.files.partition(::isCodeFragmentFile)

        // Collect original declarations from the context files
        val collectingVisitor = IrDeclarationMappingCollectingVisitor()
        irOrdinaryFiles.forEach { it.acceptVoid(collectingVisitor) }

        // Replace duplicate symbols with the original ones
        val patchingVisitor = IrDeclarationPatchingVisitor(collectingVisitor.mappings)
        irCodeFragmentFiles.forEach { it.acceptVoid(patchingVisitor) }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun computeAdditionalCodeFragmentMapping(descriptor: IrBasedDeclarationDescriptor<*>): CodeFragmentCapturedValue? {
        val owner = descriptor.owner

        if (descriptor is IrBasedReceiverParameterDescriptor && owner is IrValueParameter) {
            val receiverClass = (owner.type as? IrSimpleType)?.classifier as? IrClassSymbol
            val receiverClassId = receiverClass?.owner?.classId

            if (receiverClassId != null) {
                if (owner.indexInOldValueParameters >= 0) {
                    val labelName = receiverClassId.shortClassName
                    return CodeFragmentCapturedValue.ContextReceiver(owner.indexInOldValueParameters, labelName, isCrossingInlineBounds = true)
                }

                val parent = owner.parent
                if (parent is IrFunction) {
                    if (parent.dispatchReceiverParameter == owner) {
                        return CodeFragmentCapturedValue.ContainingClass(receiverClassId, isCrossingInlineBounds = true)
                    }

                    return CodeFragmentCapturedValue.ExtensionReceiver(parent.name.asString(), isCrossingInlineBounds = true)
                }
            }
        }

        if (descriptor is IrBasedVariableDescriptor && owner is IrVariable) {
            val name = owner.name
            val isMutated = false // TODO capture the usage somehow

            if (owner.origin == IrDeclarationOrigin.PROPERTY_DELEGATE) {
                return CodeFragmentCapturedValue.LocalDelegate(name, isMutated, isCrossingInlineBounds = true)
            }

            return CodeFragmentCapturedValue.Local(name, isMutated, isCrossingInlineBounds = true)
        }

        if (descriptor is IrBasedValueParameterDescriptor && owner is IrValueParameter) {
            val name = owner.name
            return CodeFragmentCapturedValue.Local(name, isMutated = false, isCrossingInlineBounds = true)
        }

        return null
    }

    private fun getFullyResolvedFirFile(file: KtFile): FirFile {
        val firFile = file.getOrBuildFirFile(firResolveSession)
        firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
        return firFile
    }

    private fun computeErrors(
        diagnostics: Collection<DiagnosticMarker>,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
    ): List<KaDiagnostic> {
        return buildList {
            for (diagnostic in diagnostics) {
                require(diagnostic is KtPsiDiagnostic)

                if (diagnostic.severity == Severity.ERROR) {
                    val ktDiagnostic = diagnostic.asKtDiagnostic()
                    if (!allowedErrorFilter(ktDiagnostic)) {
                        add(ktDiagnostic)
                    }
                }
            }
        }
    }

    private class CodeFragmentMappings(
        val capturedValues: List<CodeFragmentCapturedValue>,
        val capturedFiles: List<KtFile>,
        val injectedValueProvider: InjectedSymbolProvider
    )

    @OptIn(LLFirInternals::class)
    private fun computeCodeFragmentMappings(
        mainKtFile: KtFile,
        mainFirFile: FirFile,
        resolveSession: LLFirResolveSession,
        configuration: CompilerConfiguration,
    ): CodeFragmentMappings {
        val codeFragment = mainFirFile.codeFragment

        val capturedData = CodeFragmentCapturedValueAnalyzer.analyze(resolveSession, codeFragment)

        val capturedSymbols = capturedData.symbols
        val capturedValues = capturedSymbols.map { it.value }
        val injectedSymbols = capturedSymbols.map {
            InjectedValue(it.symbol, it.typeRef, it.value.isMutated)
        }

        val conversionData = CodeFragmentConversionData(
            classId = ClassId(FqName.ROOT, Name.identifier(configuration[CODE_FRAGMENT_CLASS_NAME] ?: "CodeFragment")),
            methodName = Name.identifier(configuration[CODE_FRAGMENT_METHOD_NAME] ?: "run"),
            injectedSymbols
        )

        val injectedSymbolMapping = injectedSymbols.associateBy {
            CodeFragmentCapturedId(it.symbol)
        }
        val injectedValueProvider = InjectedSymbolProvider(conversionData, mainKtFile, injectedSymbolMapping)

        return CodeFragmentMappings(capturedValues, capturedData.files, injectedValueProvider)
    }

    private class InjectedSymbolProvider(
        val conversionData: CodeFragmentConversionData,
        private val mainKtFile: KtFile,
        private val injectedSymbolMapping: Map<CodeFragmentCapturedId, InjectedValue>
    ) : (FirReference, Fir2IrConversionScope) -> InjectedValue? {
        override fun invoke(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            val irFile = conversionScope.containingFileIfAny()
            val psiFile = (irFile?.fileEntry as? PsiIrFileEntry)?.psiFile

            if (psiFile != mainKtFile) {
                return null
            }

            val id = when (calleeReference) {
                is FirThisReference -> when (val boundSymbol = calleeReference.boundSymbol) {
                    is FirClassSymbol -> CodeFragmentCapturedId(boundSymbol)
                    is FirReceiverParameterSymbol, is FirValueParameterSymbol -> when (val referencedSymbol = calleeReference.referencedMemberSymbol) {
                        // Specific (deprecated) case for a class context receiver
                        // TODO: remove with KT-72994
                        is FirClassSymbol -> CodeFragmentCapturedId(referencedSymbol)
                        else -> CodeFragmentCapturedId(boundSymbol)
                    }
                    is FirTypeParameterSymbol, is FirTypeAliasSymbol -> errorWithFirSpecificEntries(
                        message = "Unexpected FirThisOwnerSymbol ${calleeReference::class.simpleName}", fir = boundSymbol.fir
                    )
                    null -> null
                }
                else -> calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()?.let { CodeFragmentCapturedId(it) }
            }
            return injectedSymbolMapping[id]
        }
    }

    private class CompilerFacilityJvmGeneratorExtensions(
        private val delegate: JvmGeneratorExtensions
    ) : StubGeneratorExtensions(), JvmGeneratorExtensions by delegate {
        override fun generateRawTypeAnnotationCall(): IrConstructorCall? = delegate.generateRawTypeAnnotationCall()

        /**
         * This method is used from [org.jetbrains.kotlin.backend.jvm.lower.SpecialAccessLowering.visitCall]
         * (via generateReflectiveAccessForGetter) and it is called for the private access member lowered to the getter/setter call.
         * If a private property has no getter/setter (the typical situation for simple private properties without explicitly defined
         * getter/setter) then this method is not used at all. Instead
         * [org.jetbrains.kotlin.backend.jvm.lower.SpecialAccessLowering.visitGetField] (or visitSetField) generates the access without
         * asking.
         */
        override fun isAccessorWithExplicitImplementation(accessor: IrSimpleFunction): Boolean {
            if (accessor is AbstractFir2IrLazyDeclaration<*>) {
                val fir = accessor.fir
                if (fir is FirFunction && fir.hasBody) {
                    return true
                }
            }
            return false
        }
    }

    private class CompilerFacilityFir2IrExtensions(
        delegate: Fir2IrExtensions,
        private val injectedValueProvider: InjectedSymbolProvider?,
    ) : Fir2IrExtensions by delegate {
        override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            return injectedValueProvider?.invoke(calleeReference, conversionScope)
        }

        override fun codeFragmentConversionData(fragment: FirCodeFragment): CodeFragmentConversionData {
            return injectedValueProvider?.conversionData ?: errorWithFirSpecificEntries("Conversion data is not provided", fir = fragment)
        }
    }

    private class SingleFileGenerateClassFilter(
        private val files: List<KtFile>,
        private val inlinedClasses: Set<KtClassOrObject>
    ) : GenerationState.GenerateClassFilter() {
        private val filesWithInlinedClasses = inlinedClasses.mapTo(mutableSetOf()) { it.containingKtFile }

        override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
            return ktFile in files || ktFile in filesWithInlinedClasses
        }

        override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
            return processingClassOrObject.containingKtFile in files ||
                    processingClassOrObject is KtObjectDeclaration && processingClassOrObject in inlinedClasses
        }
    }

    private fun createJvmIrCodegenFactory(
        configuration: CompilerConfiguration,
        isCodeFragment: Boolean,
        irModuleFragment: IrModuleFragment,
    ): JvmIrCodegenFactory {
        val jvmGeneratorExtensions = object : JvmGeneratorExtensionsImpl(configuration) {
            override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
                // Stubbed top-level function IR symbols (from other source files in the module) require a parent facade class to be
                // generated, which requires a container source to be provided. Without a facade class, function IR symbols will have
                // an `IrExternalPackageFragment` parent, which trips up code generation during IR lowering.
                val psiSourceFile =
                    descriptor.toSourceElement.containingFile as? PsiSourceFile ?: return super.getContainerSource(descriptor)
                return FacadeClassSourceShimForFragmentCompilation(psiSourceFile)
            }
        }

        val ideCodegenSettings = JvmIrCodegenFactory.IdeCodegenSettings(
            shouldStubAndNotLinkUnboundSymbols = true,
            shouldDeduplicateBuiltInSymbols = false,

            // Because the file to compile may be contained in a "common" multiplatform module, an `expect` declaration doesn't necessarily
            // have an obvious associated `actual` symbol. `shouldStubOrphanedExpectSymbols` generates stubs for such `expect` declarations.
            shouldStubOrphanedExpectSymbols = true,

            // Likewise, the file to compile may be contained in a "platform" multiplatform module, where the `actual` declaration is
            // referenced in the symbol table automatically, but not its `expect` counterpart, because it isn't contained in the files to
            // compile. `shouldReferenceUndiscoveredExpectSymbols` references such `expect` symbols in the symbol table so that they can
            // subsequently be stubbed.
            shouldReferenceUndiscoveredExpectSymbols = false, // TODO it was true
        )

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        val evaluatorFragmentInfoForPsi2Ir = runIf<EvaluatorFragmentInfo?>(isCodeFragment) {
            val irFile = irModuleFragment.files.single { (it.fileEntry as? PsiIrFileEntry)?.psiFile is KtCodeFragment }
            val irClass = irFile.declarations.single { it is IrClass && it.metadata is FirMetadataSource.CodeFragment } as IrClass
            val irFunction = irClass.declarations.single { it is IrFunction && it !is IrConstructor } as IrFunction
            EvaluatorFragmentInfo(irClass.descriptor, irFunction.descriptor, emptyList())
        }

        return JvmIrCodegenFactory(
            configuration,
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            evaluatorFragmentInfoForPsi2Ir = evaluatorFragmentInfoForPsi2Ir,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}

private class IrDeclarationMappingCollectingVisitor : IrElementVisitorVoid {
    private val collectedMappings = HashMap<FirDeclaration, IrDeclaration>()

    val mappings: Map<FirDeclaration, IrDeclaration>
        get() = Collections.unmodifiableMap(collectedMappings)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        dumpDeclaration(declaration)
        super.visitDeclaration(declaration)
    }

    private fun dumpDeclaration(declaration: IrDeclaration) {
        if (declaration is IrMetadataSourceOwner) {
            val fir = (declaration.metadata as? FirMetadataSource)?.fir
            if (fir != null) {
                collectedMappings.putIfAbsent(fir, declaration)
            }
        }
    }
}

private class IrDeclarationPatchingVisitor(private val mapping: Map<FirDeclaration, IrDeclaration>) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        patchIfNeeded(expression.superQualifierSymbol) { expression.superQualifierSymbol = it }
        super.visitFieldAccess(expression)
    }

    override fun visitValueAccess(expression: IrValueAccessExpression) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        super.visitValueAccess(expression)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        super.visitGetEnumValue(expression)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        super.visitGetObjectValue(expression)
    }

    override fun visitCall(expression: IrCall) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        patchIfNeeded(expression.superQualifierSymbol) { expression.superQualifierSymbol = it }
        super.visitCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        super.visitConstructorCall(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        patchIfNeeded(expression.getter) { expression.getter = it }
        patchIfNeeded(expression.setter) { expression.setter = it }
        super.visitPropertyReference(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        patchIfNeeded(expression.reflectionTarget) { expression.reflectionTarget = it }
        super.visitFunctionReference(expression)
    }

    override fun visitClassReference(expression: IrClassReference) {
        patchIfNeeded(expression.symbol) { expression.symbol = it }
        super.visitClassReference(expression)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private inline fun <reified T : IrSymbol> patchIfNeeded(irSymbol: T?, patcher: (T) -> Unit) {
        if (irSymbol != null) {
            val irDeclaration = irSymbol.owner as? IrMetadataSourceOwner ?: return
            val firDeclaration = (irDeclaration.metadata as? FirMetadataSource)?.fir ?: return
            val correctedIrSymbol = mapping[firDeclaration]?.symbol as? T ?: return
            if (correctedIrSymbol != irSymbol) {
                patcher(correctedIrSymbol)
            }
        }
    }
}

private class SyntaxErrorReportingVisitor(
    private val useSiteSession: FirSession,
    private val diagnosticConverter: (KtPsiDiagnostic) -> KaDiagnosticWithPsi<*>
) : KtTreeVisitorVoid() {
    private val collectedDiagnostics = mutableListOf<KaDiagnostic>()

    val diagnostics: List<KaDiagnostic>
        get() = Collections.unmodifiableList(collectedDiagnostics)

    override fun visitErrorElement(element: PsiErrorElement) {
        collectedDiagnostics += ConeSyntaxDiagnostic(element.errorDescription)
            .toFirDiagnostics(useSiteSession, KtRealPsiSourceElement(element), callOrAssignmentSource = null)
            .map { diagnosticConverter(it as KtPsiDiagnostic) }

        super.visitErrorElement(element)
    }
}
