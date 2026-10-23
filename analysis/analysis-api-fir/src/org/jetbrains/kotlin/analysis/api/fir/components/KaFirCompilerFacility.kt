/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.analysis.api.compilation.KaCodeCompilationException
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptions
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptionsBuilder
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.compilation.KaFirCompilationOptions
import org.jetbrains.kotlin.analysis.api.fir.compilation.KaFirCompilationOptionsBuilder
import org.jetbrains.kotlin.analysis.api.fir.components.compilation.*
import org.jetbrains.kotlin.analysis.api.fir.utils.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsCompilerFacility
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleOutputProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.baseContextModuleOrSelf
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.diagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerData
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmEvaluatorData
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.codegen.state.CompiledCodeProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.pipeline.IrGenerationExtensionException
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirCompilerFacility(
    override val analysisSessionProvider: () -> KaFirSession
) : KaFirSessionComponent(), KaInternalsCompilerFacility {
    override fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult = withPsiValidityAssertion(file) {
        val opts = options as KaFirCompilationOptions
        return withPsiValidityAssertion(file) {
            compileWithRetry(file, opts)
        }
    }

    override fun createCompilationOptions(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions = withValidityAssertion {
        return KaFirCompilationOptionsBuilder(token, CompilerConfiguration.create()).apply(init).build()
    }

    override fun modify(options: KaCompilationOptions, init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions = withValidityAssertion {
        return (options as KaFirCompilationOptions).modify(init)
    }

    private fun compileWithRetry(
        file: KtFile,
        options: KaFirCompilationOptions,
    ): KaCompilationResult {
        val disabledIrExtensions = mutableListOf<Class<out IrGenerationExtension>>()
        val mutedExceptions = mutableListOf<Throwable>()
        while (true) {
            try {
                val result = compileUnsafe(file, options, disabledIrExtensions)
                if (mutedExceptions.isNotEmpty()) {
                    return when (result) {
                        is KaCompilationResult.Failure -> KaCompilationResult.Failure(
                            errors = result.errors,
                            mutedExceptions = mutedExceptions,
                        )
                        is KaCompilationResult.Success -> KaCompilationResult.Success(
                            output = result.output,
                            capturedValues = result.capturedValues,
                            canBeCached = result.canBeCached,
                            mutedExceptions = mutedExceptions,
                        )
                    }
                }
                return result
            } catch (e: IrGenerationExtensionException) {
                val extensionClass = e.extensionClass
                if (disabledIrExtensions.contains(extensionClass)) {
                    throw KaCodeCompilationException(e) // very strange
                }
                mutedExceptions.add(e)
                disabledIrExtensions.add(extensionClass)
            } catch (e: Throwable) {
                rethrowIntellijPlatformExceptionIfNeeded(e)
                throw KaCodeCompilationException(e)
            }
        }
    }

    private fun compileUnsafe(
        mainFile: KtFile,
        options: KaFirCompilationOptions,
        disabledIrExtensions: List<Class<out IrGenerationExtension>>,
    ): KaCompilationResult {
        val syntaxErrors = SyntaxErrorReportingVisitor(analysisSession.firSession) { it.asKaDiagnostic() }
            .also(mainFile::accept).diagnostics

        if (syntaxErrors.isNotEmpty()) {
            return KaCompilationResult.Failure(syntaxErrors)
        }

        val mainFirFile = getFullyResolvedFirFile(mainFile, resolutionFacade)

        val inlineStackData = retrieveInlineStackData(mainFirFile, resolutionFacade, options.jvmExecutionStack)

        val codeFragmentMappings = runIf(mainFile is KtCodeFragment) {
            computeCodeFragmentMappings(
                mainFirFile,
                resolutionFacade,
                options.codeFragmentClassName,
                options.codeFragmentMethodName,
                inlineStackData.capturedReifiedTypeParameterMapping,
                inlineStackData.inlineLambdaParameterMapping
            )
        }

        val actualizer = createPlatformActualizer(options.moduleActualizer)
        val compilationPeerData = CompilationPeerCollector.process(
            buildList {
                add(mainFirFile)
                codeFragmentMappings?.capturedFiles?.forEach { add(getFullyResolvedFirFile(it, resolutionFacade)) }
            },
            actualizer
        )

        val chunkRegistrar = CompilationChunkRegistrar(mainFile, mainFirFile, actualizer, project, resolutionFacade)
        val chunks = collectCompilationChunks(chunkRegistrar, compilationPeerData, codeFragmentMappings)

        val registeredCodeProviders = ArrayList<CompiledCodeProvider>()

        val contextDeclarationCache = if (codeFragmentMappings != null) {
            // A code fragment may be moved to a different dangling file module, so here we cannot use the 'mainFile'
            val effectiveCodeFragment = chunks.values.last().mainFile as KtCodeFragment
            val contextDeclaration = effectiveCodeFragment.context?.getNonLocalContainingOrThisDeclarationWithIrMetadata()
            if (contextDeclaration != null) {
                CodeFragmentContextDeclarationCache(
                    contextDeclaration,
                    inlineStackData.firstNonInlineNonLocalFunInStack,
                    codeFragmentMappings.selfSymbols
                )
            } else null
        } else null

        val nonLocalReturnErrors = detectNonLocalReturnsInEvaluatingLambdas(inlineStackData)
        if (nonLocalReturnErrors.isNotEmpty()) {
            return KaCompilationResult.Failure(nonLocalReturnErrors)
        }

        for ([module, chunk] in chunks) {
            ProgressManager.checkCanceled()

            val mainFile = chunk.mainFile
            if (mainFile != null) {
                // Do not check dependency files – even though there might be errors, it's OK as long as they don't affect the main file.
                // This is important for the code evaluation scenario, as people may modify code while debugging.
                // The downside is that we can get unexpected exceptions from the backend (that we wrap into KaCompilationResult.Failure).
                //
                // The chunk file might belong to another module than the use-site one (e.g., a code fragment which was moved to a different
                // dangling file module), so the module-agnostic 'LLResolutionFacade' is used here instead of the Analysis API endpoints.
                val diagnostics = mainFile.diagnostics(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                    .filter { !it.isSuppressed }
                    .map { it.diagnostic }

                val errors = computeErrors(diagnostics.filterIsInstance<KtDiagnostic>().toList(), options.allowedErrorFilter)
                if (errors.isNotEmpty()) {
                    return KaCompilationResult.Failure(errors)
                }
            }

            if (chunk.attachPrecompiledBinaries) {
                val targetModule = module.baseContextModuleOrSelf as? KaSourceModule
                if (targetModule != null) {
                    val outputDirectory = KotlinModuleOutputProvider.getInstance(project).getCompilationOutput(targetModule)
                    if (outputDirectory != null) {
                        registeredCodeProviders += KaFirDirectoryBasedCompiledCodeProvider(outputDirectory)
                    }
                }

                if (chunk.files.isEmpty()) {
                    continue
                }
            }

            val result = compileChunk(
                module,
                chunk,
                options,
                codeFragmentMappings?.takeIf { chunk.hasCodeFragments },
                KaFirDelegatingCompiledCodeProvider(registeredCodeProviders),
                contextDeclarationCache,
                disabledIrExtensions,
            )

            when (result) {
                is KaCompilationResult.Failure -> return result
                is KaCompilationResult.Success if chunk.kind == ChunkKind.MAIN -> return result
                is KaCompilationResult.Success -> {
                    val classMap = buildMap {
                        for (compiledFile in result.output) {
                            val className = getInternalClassName(compiledFile.path) ?: continue

                            // `GenerationState.inlineCache` uses the path to class files without ".class" as a key.
                            // For example,
                            //  - The key for `Foo` class in `com.example.foo` package is `com/example/foo/Foo`.
                            //  - The key for a companion object of `Foo` in `com.example.foo` package is `com/example/foo/Foo$Companion`.
                            //  - The key for an inner class `Inner` of `Foo` in `com.example.foo` package is `com/example/foo/Foo$Inner`.
                            put(className, compiledFile.content)
                        }
                    }

                    registeredCodeProviders += KaFirDependencyCompiledCodeProvider(classMap)
                }
            }
        }

        errorWithAttachment("Unexpectedly skipped the main file") {
            withPsiEntry("file", mainFile)
        }
    }

    private fun detectNonLocalReturnsInEvaluatingLambdas(inlineStackData: InlineStackData): List<KaDiagnostic> {
        val reporter = DiagnosticsCollectorImpl()
        inlineStackData.inlineLambdaParameterMapping.values.forEach {

            val lambda = it.expr as? FirAnonymousFunctionExpression ?: return@forEach

            val context = object : DiagnosticContext {
                override val containingFile: KtSourceFile?
                    get() = lambda.psi?.containingFile?.let(::KtPsiSourceFile)

                override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic) = false

                override val languageVersionSettings: LanguageVersionSettings
                    get() = analysisSession.firSession.languageVersionSettings
            }

            lambda.accept(object : FirDefaultVisitorVoid() {

                val allowedReturnTargets = mutableListOf<FirTargetElement>()

                override fun visitElement(element: FirElement) {
                    if (element is FirLoop || element is FirFunction) {
                        allowedReturnTargets.add(element)
                    }
                    element.acceptChildren(this)
                    if (element is FirLoop || element is FirFunction) {
                        allowedReturnTargets.removeLast()
                    }
                }

                override fun visitReturnExpression(returnExpression: FirReturnExpression) {
                    val targetElement = returnExpression.target.labeledElement
                    if (targetElement !in allowedReturnTargets) {
                        reporter.reportOn(returnExpression.source, FirErrors.RETURN_NOT_ALLOWED, context)
                    }
                    returnExpression.acceptChildren(this)
                }
            }, null)
        }
        return computeErrors(reporter.diagnostics) { false }
    }

    private fun getInternalClassName(classFilePath: String): String? {
        if (classFilePath.endsWith(".class", ignoreCase = true)) {
            return classFilePath.dropLast(".class".length)
        }
        return null
    }

    private fun collectCompilationChunks(
        chunkRegistrar: CompilationChunkRegistrar,
        compilationPeerData: CompilationPeerData,
        codeFragmentMappings: CodeFragmentMappings?
    ): Map<KaModule, ChunkToCompile> {
        for ([module, files] in compilationPeerData.peers) {
            for (file in files) {
                chunkRegistrar.submit(file, module)
            }
        }

        if (codeFragmentMappings != null) {
            for (capturedFile in codeFragmentMappings.capturedFiles) {
                val module = resolutionFacade.getModule(capturedFile)
                chunkRegistrar.submit(capturedFile, module)
            }
        }

        return chunkRegistrar.computeChunks()
    }

    private fun compileChunk(
        module: KaModule,
        chunk: ChunkToCompile,
        options: KaFirCompilationOptions,
        codeFragmentMappings: CodeFragmentMappings?,
        compiledCodeProvider: CompiledCodeProvider,
        contextDeclarationCache: CodeFragmentContextDeclarationCache?,
        disabledIrExtensions: List<Class<out IrGenerationExtension>>,
    ): KaCompilationResult {
        val session = resolutionFacade.sessionProvider.getResolvableSession(module)
        val configuration = options.configuration.copy().apply {
            put(CommonConfigurationKeys.USE_FIR, true)
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, session.languageVersionSettings)
        }

        val baseFir2IrExtensions = JvmFir2IrExtensions()

        val fir2IrExtensions = when {
            codeFragmentMappings != null && chunk.mainFile != null -> {
                val injectedValueProvider = InjectedSymbolProvider(codeFragmentMappings, chunk.mainFile)
                CodeFragmentFir2IrExtensions(
                    baseFir2IrExtensions,
                    injectedValueProvider,
                    codeFragmentMappings.inlineLambdaParametersMapping
                )
            }
            chunk.kind == ChunkKind.CONTEXT -> {
                CodeFragmentContextFir2IrExtensions(baseFir2IrExtensions, contextDeclarationCache)
            }
            else -> baseFir2IrExtensions
        }

        val diagnosticsCollector = DiagnosticsCollectorImpl()

        val commonMemberStorage = contextDeclarationCache?.customCommonMemberStorage ?: Fir2IrCommonMemberStorage()

        val irGeneratorExtensions = getIrGenerationExtensions(module).filter { !disabledIrExtensions.contains(it.javaClass) }

        val fir2IrResult = runFir2Ir(
            session = session,
            firFiles = chunk.files.map { it.firFile },
            fir2IrExtensions = fir2IrExtensions,
            diagnosticReporter = diagnosticsCollector,
            effectiveConfiguration = configuration,
            irGeneratorExtensions = irGeneratorExtensions,
            commonMemberStorage = commonMemberStorage
        )

        val convertedMapping = codeFragmentMappings?.reifiedTypeParametersMapping.orEmpty().entries.associate { [firTypeParam, coneType] ->
            val irTypeParam = fir2IrResult.components.classifierStorage.getIrTypeParameterSymbol(firTypeParam, ConversionTypeOrigin.DEFAULT)
            irTypeParam to with(fir2IrResult.components) { coneType.toIrType() }
        }

        if (diagnosticsCollector.hasErrors) {
            val errors = computeErrors(diagnosticsCollector.diagnostics.filterIsInstance<KtDiagnosticWithSource>(), options.allowedErrorFilter)
            if (errors.isNotEmpty()) {
                return KaCompilationResult.Failure(errors)
            }
        }

        val evaluatorData = when (chunk.kind) {
            ChunkKind.MAIN if codeFragmentMappings != null -> {
                val irFile = fir2IrResult.irModuleFragment.files.single { (it.fileEntry as? PsiIrFileEntry)?.psiFile is KtCodeFragment }
                val irClass = irFile.declarations.single { it is IrClass && it.metadata is FirMetadataSource.CodeFragment } as IrClass
                val irFunction = irClass.declarations.single { it is IrFunction && it !is IrConstructor } as IrFunction

                val localDeclarationsData = contextDeclarationCache?.localDeclarationsData
                    ?: JvmBackendContext.SharedLocalDeclarationsData()

                JvmEvaluatorData(
                    localDeclarationsData = localDeclarationsData,
                    evaluatorGeneratedFunction = irFunction,
                    capturedTypeParametersMapping = convertedMapping
                )
            }
            ChunkKind.CONTEXT -> {
                JvmEvaluatorData(
                    localDeclarationsData = JvmBackendContext.SharedLocalDeclarationsData(),
                    evaluatorGeneratedFunction = null,
                    capturedTypeParametersMapping = convertedMapping
                )
            }
            else -> null
        }

        val codegenFactory = createJvmIrCodegenFactory(configuration, evaluatorData)

        val result = runJvmIrCodeGen(
            chunk,
            fir2IrResult,
            configuration,
            options,
            codeFragmentMappings,
            codegenFactory,
            diagnosticsCollector,
            compiledCodeProvider
        )

        if (diagnosticsCollector.hasErrors) {
            val errors = computeErrors(diagnosticsCollector.diagnostics.filterIsInstance<KtDiagnosticWithSource>(), options.allowedErrorFilter)
            if (errors.isNotEmpty()) {
                return KaCompilationResult.Failure(errors)
            }
        }

        if (chunk.kind == ChunkKind.CONTEXT) {
            contextDeclarationCache?.initialize(commonMemberStorage, evaluatorData)
        }

        return result
    }

    private fun computeErrors(
        diagnostics: Collection<KtDiagnostic>,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
    ): List<KaDiagnostic> {
        return buildList {
            for (diagnostic in diagnostics) {
                if (diagnostic.severity == Severity.ERROR) {
                    val ktDiagnostic = when (diagnostic) {
                        is KtDiagnosticWithSource -> diagnostic.asKaDiagnostic()
                        else -> {
                            val message = diagnostic.renderMessage()
                            KaNonBoundToPsiErrorDiagnostic(diagnostic.factoryName, message, analysisSession.token)
                        }
                    }
                    if (!allowedErrorFilter(ktDiagnostic)) {
                        add(ktDiagnostic)
                    }
                }
            }
        }
    }
}
