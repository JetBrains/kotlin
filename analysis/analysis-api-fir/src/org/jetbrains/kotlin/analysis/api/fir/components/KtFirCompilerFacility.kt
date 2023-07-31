/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.components.KtCompilerFacility
import org.jetbrains.kotlin.analysis.api.components.KtCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KtCompilerTarget
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.util.KtCompiledFileForOutputFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirWholeFileResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.resolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticMarker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.pipeline.applyIrGenerationExtensions
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class KtFirCompilerFacility(
    override val analysisSession: KtFirAnalysisSession
) : KtCompilerFacility(), KtFirAnalysisSessionComponent {
    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KtCompilerTarget,
        allowedErrorFilter: (KtDiagnostic) -> Boolean
    ): KtCompilationResult {
        val classBuilderFactory = when (target) {
            is KtCompilerTarget.Jvm -> target.classBuilderFactory
        }

        val effectiveConfiguration = configuration
            .copy()
            .apply {
                put(CommonConfigurationKeys.USE_FIR, true)
            }

        val mainFirFile = getFullyResolvedFirFile(file)

        val frontendDiagnostics = file.collectDiagnosticsForFile(firResolveSession, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        val frontendErrors = computeErrors(frontendDiagnostics, allowedErrorFilter)

        if (frontendErrors.isNotEmpty()) {
            return KtCompilationResult.Failure(frontendErrors)
        }

        val codeFragmentMappings = runIf(file is KtCodeFragment) {
            computeCodeFragmentMappings(file, mainFirFile, firResolveSession, configuration)
        }

        val compilationPeerData = CompilationPeerCollector.process(mainFirFile)

        val filesToCompile = buildList {
            val dependencyFiles = buildSet {
                addAll(compilationPeerData.files)
                addAll(codeFragmentMappings?.capturedFiles.orEmpty())

                // The main file needs to be the last so caches for the context declarations are populated in FIR-to-IR.
                remove(file)
            }

            addAll(dependencyFiles)
            add(file)
        }

        val firFilesToCompile = filesToCompile.map(::getFullyResolvedFirFile)

        val generateClassFilter = SingleFileGenerateClassFilter(file, compilationPeerData.inlinedClasses)

        val jvmGeneratorExtensions = JvmFir2IrExtensions(effectiveConfiguration, JvmIrDeserializerImpl(), JvmIrMangler)
        val fir2IrExtensions = CompilerFacilityFir2IrExtensions(jvmGeneratorExtensions, codeFragmentMappings?.injectedValueProvider)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val fir2IrConfiguration = Fir2IrConfiguration(
            effectiveConfiguration.languageVersionSettings,
            diagnosticsReporter,
            linkViaSignatures = false,
            effectiveConfiguration[CommonConfigurationKeys.EVALUATED_CONST_TRACKER] ?: EvaluatedConstTracker.create(),
            effectiveConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
            allowNonCachedDeclarations = true
        )

        val fir2IrResult = Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            rootModuleSession,
            firResolveSession.getScopeSessionFor(rootModuleSession),
            firFilesToCompile,
            fir2IrExtensions,
            fir2IrConfiguration,
            JvmIrMangler,
            IrFactoryImpl,
            FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider(),
            DefaultBuiltIns.Instance,
            Fir2IrCommonMemberStorage(signatureComposerForJvmFir2Ir(false), FirJvmKotlinMangler()),
            initializedIrBuiltIns = null
        )

        ProgressManager.checkCanceled()

        val irGeneratorExtensions = IrGenerationExtension.getInstances(project)
        fir2IrResult.components.applyIrGenerationExtensions(fir2IrResult.irModuleFragment, irGeneratorExtensions)

        val bindingContext = NoScopeRecordCliBindingTrace().bindingContext
        val codegenFactory = createJvmIrCodegenFactory(effectiveConfiguration, file is KtCodeFragment, fir2IrResult.irModuleFragment)

        val generationState = GenerationState.Builder(
            project,
            classBuilderFactory,
            fir2IrResult.irModuleFragment.descriptor,
            bindingContext,
            filesToCompile,
            effectiveConfiguration,
        ).generateDeclaredClassFilter(generateClassFilter)
            .codegenFactory(codegenFactory)
            .diagnosticReporter(diagnosticsReporter)
            .build()

        try {
            generationState.beforeCompile()

            ProgressManager.checkCanceled()

            codegenFactory.generateModuleInFrontendIRMode(
                generationState,
                fir2IrResult.irModuleFragment,
                fir2IrResult.components.symbolTable,
                fir2IrResult.components.irProviders,
                CompilerFacilityJvmGeneratorExtensions(jvmGeneratorExtensions),
                FirJvmBackendExtension(fir2IrResult.components, null),
                fir2IrResult.pluginContext
            )

            CodegenFactory.doCheckCancelled(generationState)
            generationState.factory.done()

            val backendDiagnostics = generationState.collectedExtraJvmDiagnostics.all()
            val backendErrors = computeErrors(backendDiagnostics, allowedErrorFilter)

            if (backendErrors.isNotEmpty()) {
                return KtCompilationResult.Failure(backendErrors)
            }

            val outputFiles = generationState.factory.asList().map(::KtCompiledFileForOutputFile)
            val capturedValues = codeFragmentMappings?.capturedValues ?: emptyList()
            return KtCompilationResult.Success(outputFiles, capturedValues)
        } finally {
            generationState.destroy()
        }
    }

    private fun getFullyResolvedFirFile(file: KtFile): FirFile {
        val firFile = file.getOrBuildFirFile(firResolveSession)
        LLFirWholeFileResolveTarget(firFile).resolve(FirResolvePhase.BODY_RESOLVE)
        return firFile
    }

    private fun computeErrors(
        diagnostics: Collection<DiagnosticMarker>,
        allowedErrorFilter: (KtDiagnostic) -> Boolean,
    ): List<KtDiagnostic> {
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
        val injectedSymbols = capturedSymbols.map { InjectedValue(it.symbol, it.typeRef, it.value.isMutated) }

        codeFragment.conversionData = CodeFragmentConversionData(
            classId = ClassId(FqName.ROOT, Name.identifier(configuration[CODE_FRAGMENT_CLASS_NAME] ?: "CodeFragment")),
            methodName = Name.identifier(configuration[CODE_FRAGMENT_METHOD_NAME] ?: "run"),
            injectedSymbols
        )

        val injectedSymbolMapping = injectedSymbols.associateBy { it.symbol }
        val injectedValueProvider = InjectedSymbolProvider(mainKtFile, injectedSymbolMapping)

        return CodeFragmentMappings(capturedValues, capturedData.files, injectedValueProvider)
    }

    private class InjectedSymbolProvider(
        private val mainKtFile: KtFile,
        private val injectedSymbolMapping: Map<FirBasedSymbol<*>, InjectedValue>
    ) : (FirReference, Fir2IrConversionScope) -> InjectedValue? {
        override fun invoke(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            val irFile = conversionScope.containingFileIfAny()
            val psiFile = (irFile?.fileEntry as? PsiIrFileEntry)?.psiFile

            if (psiFile != mainKtFile) {
                return null
            }

            val symbol = when (calleeReference) {
                is FirThisReference -> calleeReference.boundSymbol
                else -> calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()
            }
            return injectedSymbolMapping[symbol]
        }
    }

    private class CompilerFacilityJvmGeneratorExtensions(
        private val delegate: JvmGeneratorExtensions
    ) : StubGeneratorExtensions(), JvmGeneratorExtensions by delegate {
        override val rawTypeAnnotationConstructor: IrConstructor?
            get() = delegate.rawTypeAnnotationConstructor
    }

    private class CompilerFacilityFir2IrExtensions(
        delegate: Fir2IrExtensions,
        private val injectedValueProvider: InjectedSymbolProvider?
    ) : Fir2IrExtensions by delegate {
        override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            return injectedValueProvider?.invoke(calleeReference, conversionScope)
        }
    }

    private class SingleFileGenerateClassFilter(
        private val file: KtFile,
        private val inlinedClasses: Set<KtClassOrObject>
    ) : GenerationState.GenerateClassFilter() {
        private val filesWithInlinedClasses = inlinedClasses.mapTo(mutableSetOf()) { it.containingKtFile }

        override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
            return file === ktFile || ktFile in filesWithInlinedClasses
        }

        override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
            return true
        }

        override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
            return processingClassOrObject.containingKtFile === file ||
                    processingClassOrObject is KtObjectDeclaration && processingClassOrObject in inlinedClasses
        }

        override fun shouldGenerateScript(script: KtScript): Boolean {
            return script.containingKtFile === file
        }

        override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
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

        val phaseConfig = PhaseConfig(if (isCodeFragment) jvmFragmentLoweringPhases else jvmLoweringPhases)

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        val evaluatorFragmentInfoForPsi2Ir = runIf<EvaluatorFragmentInfo?>(isCodeFragment) {
            val irFile = irModuleFragment.files.single { (it.fileEntry as? PsiIrFileEntry)?.psiFile is KtCodeFragment }
            val irClass = irFile.declarations.single { it is IrClass && it.metadata is FirMetadataSource.CodeFragment } as IrClass
            val irFunction = irClass.declarations.single { it is IrFunction && it !is IrConstructor } as IrFunction
            EvaluatorFragmentInfo(irClass.descriptor, irFunction.descriptor, emptyList())
        }

        return JvmIrCodegenFactory(
            configuration,
            phaseConfig,
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            evaluatorFragmentInfoForPsi2Ir = evaluatorFragmentInfoForPsi2Ir,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}