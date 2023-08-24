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
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedId
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
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.pipeline.applyIrGenerationExtensions
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedValueParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbolInternals
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.util.Collections

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

        patchCodeFragmentIr(fir2IrResult)

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
            return KtCompilationResult.Success(outputFiles, capturedValues)
        } finally {
            generationState.destroy()
        }
    }

    private fun patchCodeFragmentIr(fir2IrResult: Fir2IrResult) {
        fun isCodeFragmentFile(irFile: IrFile): Boolean {
            val firFiles = (irFile.metadata as? FirMetadataSource.File)?.files ?: return false
            return firFiles.any { it.psi is KtCodeFragment }
        }

        val (irCodeFragmentFiles, irOrdinaryFiles) = fir2IrResult.irModuleFragment.files.partition(::isCodeFragmentFile)

        // Collect original declarations from the context files
        val collectingVisitor = IrDeclarationMappingCollectingVisitor()
        irOrdinaryFiles.forEach { it.acceptVoid(collectingVisitor) }

        // Replace duplicate symbols with the original ones
        val patchingVisitor = IrDeclarationPatchingVisitor(collectingVisitor.mappings)
        irCodeFragmentFiles.forEach { it.acceptVoid(patchingVisitor) }
    }

    @OptIn(IrSymbolInternals::class)
    private fun computeAdditionalCodeFragmentMapping(descriptor: IrBasedDeclarationDescriptor<*>): CodeFragmentCapturedValue? {
        val owner = descriptor.owner

        if (descriptor is IrBasedReceiverParameterDescriptor && owner is IrValueParameter) {
            val receiverClass = (owner.type as? IrSimpleType)?.classifier as? IrClassSymbol
            val receiverClassId = receiverClass?.owner?.classId

            if (receiverClassId != null) {
                if (owner.index >= 0) {
                    val labelName = receiverClassId.shortClassName
                    return CodeFragmentCapturedValue.ContextReceiver(owner.index, labelName, isCrossingInlineBounds = true)
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
        val injectedSymbols = capturedSymbols.map {
            InjectedValue(it.symbol, it.contextReceiverNumber, it.typeRef, it.value.isMutated)
        }

        codeFragment.conversionData = CodeFragmentConversionData(
            classId = ClassId(FqName.ROOT, Name.identifier(configuration[CODE_FRAGMENT_CLASS_NAME] ?: "CodeFragment")),
            methodName = Name.identifier(configuration[CODE_FRAGMENT_METHOD_NAME] ?: "run"),
            injectedSymbols
        )

        val injectedSymbolMapping = injectedSymbols.associateBy {
            CodeFragmentCapturedId(it.symbol, it.contextReceiverNumber)
        }
        val injectedValueProvider = InjectedSymbolProvider(mainKtFile, injectedSymbolMapping)

        return CodeFragmentMappings(capturedValues, capturedData.files, injectedValueProvider)
    }

    private class InjectedSymbolProvider(
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
                is FirThisReference -> calleeReference.boundSymbol?.let {
                    CodeFragmentCapturedId(it, calleeReference.contextReceiverNumber)
                }
                else -> calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()?.let { CodeFragmentCapturedId(it) }
            }
            return injectedSymbolMapping[id]
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

    @OptIn(IrSymbolInternals::class)
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
