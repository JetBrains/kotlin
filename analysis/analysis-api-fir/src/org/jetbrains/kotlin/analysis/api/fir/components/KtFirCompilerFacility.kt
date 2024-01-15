/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.components.KtCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KtCompilerFacility
import org.jetbrains.kotlin.analysis.api.components.KtCompilerTarget
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.util.KtCompiledFileForOutputFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedId
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.*
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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.jetbrains.kotlin.ir.util.*
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
import java.util.*

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

        val syntaxErrors = SyntaxErrorReportingVisitor().also(file::accept).diagnostics

        if (syntaxErrors.isNotEmpty()) {
            return KtCompilationResult.Failure(syntaxErrors)
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

        // Files in the code fragment context module are compiled together with the code fragment itself.
        val targetModules = computeTargetModules(mainFirFile.llFirModuleData.ktModule)
        val (targetFiles, dependencyFiles) = filesToCompile.partition { firResolveSession.getModule(it) in targetModules }
        require(targetFiles.isNotEmpty())

        val jvmIrDeserializer = JvmIrDeserializerImpl()
        val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter()

        val irGeneratorExtensions = IrGenerationExtension.getInstances(project)

        val dependencyFir2IrResults = dependencyFiles
            .map(::getFullyResolvedFirFile)
            .groupBy { it.llFirSession }
            .map { (dependencySession, dependencyFiles) ->
                val dependencyConfiguration = configuration
                    .copy()
                    .apply {
                        put(CommonConfigurationKeys.USE_FIR, true)
                        put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, dependencySession.languageVersionSettings)
                    }

                val dependencyFir2IrExtensions = JvmFir2IrExtensions(dependencyConfiguration, jvmIrDeserializer, JvmIrMangler)
                runFir2Ir(
                    dependencySession, dependencyFiles, dependencyFir2IrExtensions,
                    diagnosticReporter, dependencyConfiguration, irGeneratorExtensions
                )
            }

        val targetConfiguration = configuration
            .copy()
            .apply {
                put(CommonConfigurationKeys.USE_FIR, true)
            }

        val jvmGeneratorExtensions = JvmFir2IrExtensions(targetConfiguration, jvmIrDeserializer, JvmIrMangler)
        val targetFir2IrExtensions = CompilerFacilityFir2IrExtensions(
            jvmGeneratorExtensions,
            dependencyFir2IrResults,
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
        targetFir2IrResult.pluginContext.applyIrGenerationExtensions(targetFir2IrResult.irModuleFragment, irGeneratorExtensions)

        val bindingContext = NoScopeRecordCliBindingTrace().bindingContext
        val codegenFactory = createJvmIrCodegenFactory(targetConfiguration, file is KtCodeFragment, targetFir2IrResult.irModuleFragment)
        val generateClassFilter = SingleFileGenerateClassFilter(file, compilationPeerData.inlinedClasses)

        val generationState = GenerationState.Builder(
            project,
            classBuilderFactory,
            targetFir2IrResult.irModuleFragment.descriptor,
            bindingContext,
            targetFiles,
            targetConfiguration,
        ).generateDeclaredClassFilter(generateClassFilter)
            .codegenFactory(codegenFactory)
            .diagnosticReporter(diagnosticReporter)
            .build()

        try {
            generationState.beforeCompile()

            ProgressManager.checkCanceled()

            codegenFactory.generateModuleInFrontendIRMode(
                generationState,
                targetFir2IrResult.irModuleFragment,
                targetFir2IrResult.components.symbolTable,
                targetFir2IrResult.components.irProviders,
                CompilerFacilityJvmGeneratorExtensions(jvmGeneratorExtensions),
                FirJvmBackendExtension(targetFir2IrResult.components, null),
                targetFir2IrResult.pluginContext
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

    private fun computeTargetModules(module: KtModule): List<KtModule> {
        return when (module) {
            is KtDanglingFileModule -> listOf(module.contextModule, module)
            else -> listOf(module)
        }
    }

    private fun runFir2Ir(
        session: LLFirSession,
        firFiles: List<FirFile>,
        fir2IrExtensions: Fir2IrExtensions,
        diagnosticReporter: DiagnosticReporter,
        effectiveConfiguration: CompilerConfiguration,
        irGeneratorExtensions: List<IrGenerationExtension>
    ): Fir2IrActualizedResult {
        val fir2IrConfiguration = Fir2IrConfiguration.forAnalysisApi(effectiveConfiguration, session.languageVersionSettings, diagnosticReporter)
        val firResult = FirResult(listOf(ModuleCompilerAnalyzedOutput(session, session.getScopeSession(), firFiles)))

        return firResult.convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            irGeneratorExtensions,
            JvmIrMangler,
            FirJvmKotlinMangler(),
            FirJvmVisibilityConverter,
            DefaultBuiltIns.Instance,
            ::JvmIrTypeSystemContext,
        )
    }

    private fun patchCodeFragmentIr(fir2IrResult: Fir2IrActualizedResult) {
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

    @OptIn(UnsafeDuringIrConstructionAPI::class)
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
        firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
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

        /**
         * This method is used from [org.jetbrains.kotlin.backend.jvm.lower.ReflectiveAccessLowering.visitCall]
         * (via generateReflectiveAccessForGetter) and it is called for the private access member lowered to the getter/setter call.
         * If a private property has no getter/setter (the typical situation for simple private properties without explicitly defined
         * getter/setter) then this method is not used at all. Instead
         * [org.jetbrains.kotlin.backend.jvm.lower.ReflectiveAccessLowering.visitGetField] (or visitSetField) generates the access without
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
        private val dependencyFir2IrResults: List<Fir2IrActualizedResult>,
        private val injectedValueProvider: InjectedSymbolProvider?
    ) : Fir2IrExtensions by delegate {
        override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
            return injectedValueProvider?.invoke(calleeReference, conversionScope)
        }

        override fun registerDeclarations(symbolTable: SymbolTable) {
            val visitor = DeclarationRegistrarVisitor(symbolTable)

            for (dependencyFir2IrResult in dependencyFir2IrResults) {
                for (dependencyFile in dependencyFir2IrResult.irModuleFragment.files) {
                    dependencyFile.acceptVoid(visitor)
                }
            }
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

private class DeclarationRegistrarVisitor(private val consumer: SymbolTable) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        register(declaration, consumer::declareClassIfNotExists)
        super.visitClass(declaration)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        register(declaration, consumer::declareTypeAliasIfNotExists)
        super.visitTypeAlias(declaration)
    }

    override fun visitScript(declaration: IrScript) {
        register(declaration, consumer::declareScript)
        super.visitScript(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        if (declaration.parent is IrClass) {
            register(declaration, consumer::declareGlobalTypeParameter)
        }

        super.visitTypeParameter(declaration)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        register(declaration, consumer::declareConstructorIfNotExists)
        super.visitConstructor(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        register(declaration, consumer::declareSimpleFunctionIfNotExists)
        super.visitSimpleFunction(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        register(declaration, consumer::declarePropertyIfNotExists)
        super.visitProperty(declaration)
    }

    override fun visitField(declaration: IrField) {
        register(declaration, consumer::declareField)
        super.visitField(declaration)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        register(declaration, consumer::declareEnumEntry)
        super.visitEnumEntry(declaration)
    }

    private inline fun <reified S : IrSymbol, D : IrDeclaration> register(
        declaration: D,
        registrar: (IdSignature, () -> S, (S) -> D) -> Unit,
    ) {
        val symbol = declaration.symbol as S
        val signature = symbol.signature ?: return
        registrar(signature, { symbol }, { declaration })
    }
}

context(KtFirAnalysisSessionComponent)
private class SyntaxErrorReportingVisitor : KtTreeVisitorVoid() {
    private val collectedDiagnostics = mutableListOf<KtDiagnostic>()

    val diagnostics: List<KtDiagnostic>
        get() = Collections.unmodifiableList(collectedDiagnostics)

    override fun visitErrorElement(element: PsiErrorElement) {
        collectedDiagnostics += ConeSyntaxDiagnostic(element.errorDescription)
            .toFirDiagnostics(analysisSession.useSiteSession, KtRealPsiSourceElement(element), callOrAssignmentSource = null)
            .map { (it as KtPsiDiagnostic).asKtDiagnostic() }

        super.visitErrorElement(element)
    }
}
