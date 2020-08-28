/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.FilteredJvmDiagnostics
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsage
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmClassCodegen
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.registerExtendedCheckersComponent
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.diagnostics.SimpleDiagnostics
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import java.io.File

object KotlinToJVMBytecodeCompiler {
    private fun writeOutput(
        configuration: CompilerConfiguration,
        outputFiles: OutputFileCollection,
        mainClassProvider: MainClassProvider?
    ) {
        val reportOutputFiles = configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        val jarPath = configuration.get(JVMConfigurationKeys.OUTPUT_JAR)
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        if (jarPath != null) {
            val includeRuntime = configuration.get(JVMConfigurationKeys.INCLUDE_RUNTIME, false)
            CompileEnvironmentUtil.writeToJar(jarPath, includeRuntime, mainClassProvider?.mainClassFqName, outputFiles)
            if (reportOutputFiles) {
                val message = OutputMessageUtil.formatOutputMessage(outputFiles.asList().flatMap { it.sourceFiles }.distinct(), jarPath)
                messageCollector.report(OUTPUT, message)
            }
            return
        }

        val outputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) ?: File(".")
        outputFiles.writeAll(outputDir, messageCollector, reportOutputFiles)
    }

    private fun createOutputFilesFlushingCallbackIfPossible(configuration: CompilerConfiguration): GenerationStateEventCallback {
        if (configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) == null) {
            return GenerationStateEventCallback.DO_NOTHING
        }
        return GenerationStateEventCallback { state ->
            val currentOutput = SimpleOutputFileCollection(state.factory.currentOutput)
            writeOutput(configuration, currentOutput, null)
            if (!configuration.get(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, false)) {
                state.factory.releaseGeneratedOutput()
            }
        }
    }

    private fun Module.getSourceFiles(
        environment: KotlinCoreEnvironment,
        localFileSystem: VirtualFileSystem,
        multiModuleChunk: Boolean,
        buildFile: File?
    ): List<KtFile> {
        return if (multiModuleChunk) {
            // filter out source files from other modules
            assert(buildFile != null) { "Compiling multiple modules, but build file is null" }
            val (moduleSourceDirs, moduleSourceFiles) =
                getBuildFilePaths(buildFile, getSourceFiles())
                    .mapNotNull(localFileSystem::findFileByPath)
                    .partition(VirtualFile::isDirectory)

            environment.getSourceFiles().filter { file ->
                val virtualFile = file.virtualFile
                virtualFile in moduleSourceFiles || moduleSourceDirs.any { dir ->
                    VfsUtilCore.isAncestor(dir, virtualFile, true)
                }
            }
        } else {
            environment.getSourceFiles()
        }
    }

    private fun CompilerConfiguration.applyModuleProperties(module: Module, buildFile: File?): CompilerConfiguration {
        return copy().apply {
            if (buildFile != null) {
                fun checkKeyIsNull(key: CompilerConfigurationKey<*>, name: String) {
                    assert(get(key) == null) { "$name should be null, when buildFile is used" }
                }

                checkKeyIsNull(JVMConfigurationKeys.OUTPUT_DIRECTORY, "OUTPUT_DIRECTORY")
                checkKeyIsNull(JVMConfigurationKeys.OUTPUT_JAR, "OUTPUT_JAR")
                put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(module.getOutputDirectory()))
            }
        }
    }

    internal fun compileModules(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>,
        repeat: Boolean = false
    ): Boolean {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val repeats = environment.configuration[CLIConfigurationKeys.REPEAT_COMPILE_MODULES]
        if (repeats != null && !repeat) {
            val performanceManager = environment.configuration[CLIConfigurationKeys.PERF_MANAGER]
            return (0 until repeats).map {
                val result = compileModules(environment, buildFile, chunk, repeat = true)
                performanceManager?.notifyRepeat(repeats, it)
                result
            }.last()
        }

        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)
        for (module in chunk) {
            moduleVisibilityManager.addModule(module)
        }

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        val projectConfiguration = environment.configuration
        if (projectConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            val extendedAnalysisMode = projectConfiguration.getBoolean(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS)
            return compileModulesUsingFrontendIR(environment, buildFile, chunk, extendedAnalysisMode)
        }

        val result = repeatAnalysisIfNeeded(analyze(environment), environment)
        if (result == null || !result.shouldGenerateCode) return false

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        result.throwIfError()

        val outputs = newLinkedHashMapWithExpectedSize<Module, GenerationState>(chunk.size)

        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = module.getSourceFiles(environment, localFileSystem, chunk.size > 1, buildFile)
            if (!checkKotlinPackageUsage(environment, ktFiles)) return false
            val moduleConfiguration = projectConfiguration.applyModuleProperties(module, buildFile)

            outputs[module] = generate(environment, moduleConfiguration, result, ktFiles, module)
        }

        return writeOutputs(environment, projectConfiguration, chunk, outputs)
    }

    private fun writeOutputs(
        environment: KotlinCoreEnvironment,
        projectConfiguration: CompilerConfiguration,
        chunk: List<Module>,
        outputs: Map<Module, GenerationState>
    ): Boolean {
        try {
            for ((_, state) in outputs) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
                val mainClassProvider = if (outputs.size == 1) MainClassProvider(state, environment) else null
                writeOutput(state.configuration, state.factory, mainClassProvider)
            }
        } finally {
            outputs.values.forEach(GenerationState::destroy)
        }

        if (projectConfiguration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)) {
            val singleModule = chunk.singleOrNull()
            if (singleModule != null) {
                return JavacWrapper.getInstance(environment.project).use {
                    it.compile(File(singleModule.getOutputDirectory()))
                }
            } else {
                projectConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                    WARNING,
                    "A chunk contains multiple modules (${chunk.joinToString { it.getModuleName() }}). " +
                            "-Xuse-javac option couldn't be used to compile java files"
                )
                JavacWrapper.getInstance(environment.project).close()
            }
        }

        return true
    }

    internal fun configureSourceRoots(configuration: CompilerConfiguration, chunk: List<Module>, buildFile: File? = null) {
        for (module in chunk) {
            val commonSources = getBuildFilePaths(buildFile, module.getCommonSourceFiles()).toSet()

            for (path in getBuildFilePaths(buildFile, module.getSourceFiles())) {
                configuration.addKotlinSourceRoot(path, isCommon = path in commonSources)
            }
        }

        for (module in chunk) {
            for ((path, packagePrefix) in module.getJavaSourceRoots()) {
                configuration.addJavaSourceRoot(File(path), packagePrefix)
            }
        }

        val isJava9Module = chunk.any { module ->
            module.getJavaSourceRoots().any { (path, packagePrefix) ->
                val file = File(path)
                packagePrefix == null &&
                        (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                                (file.isDirectory && file.listFiles().any { it.name == PsiJavaModule.MODULE_INFO_FILE }))
            }
        }

        for (module in chunk) {
            for (classpathRoot in module.getClasspathRoots()) {
                configuration.add(
                    CLIConfigurationKeys.CONTENT_ROOTS,
                    if (isJava9Module) JvmModulePathRoot(File(classpathRoot)) else JvmClasspathRoot(File(classpathRoot))
                )
            }
        }

        for (module in chunk) {
            val modularJdkRoot = module.modularJdkRoot
            if (modularJdkRoot != null) {
                // We use the SDK of the first module in the chunk, which is not always correct because some other module in the chunk
                // might depend on a different SDK
                configuration.put(JVMConfigurationKeys.JDK_HOME, File(modularJdkRoot))
                break
            }
        }

        configuration.addAll(JVMConfigurationKeys.MODULES, chunk)
    }

    private fun compileModulesUsingFrontendIR(
        environment: KotlinCoreEnvironment,
        buildFile: File?,
        chunk: List<Module>,
        extendedAnalysisMode: Boolean
    ): Boolean {
        val project = environment.project
        val performanceManager = environment.configuration.get(CLIConfigurationKeys.PERF_MANAGER)

        Extensions.getArea(project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)

        val projectConfiguration = environment.configuration
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val outputs = newLinkedHashMapWithExpectedSize<Module, GenerationState>(chunk.size)
        for (module in chunk) {
            performanceManager?.notifyAnalysisStarted()
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val ktFiles = module.getSourceFiles(environment, localFileSystem, chunk.size > 1, buildFile)
            if (!checkKotlinPackageUsage(environment, ktFiles)) return false
            val moduleConfiguration = projectConfiguration.applyModuleProperties(module, buildFile)

            val scope = GlobalSearchScope.filesScope(project, ktFiles.map { it.virtualFile })
                .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
            val provider = FirProjectSessionProvider(project)

            class FirJvmModuleInfo(override val name: Name) : ModuleInfo {
                constructor(moduleName: String) : this(Name.identifier(moduleName))

                val dependencies: MutableList<ModuleInfo> = mutableListOf()

                override val platform: TargetPlatform
                    get() = JvmPlatforms.unspecifiedJvmPlatform

                override val analyzerServices: PlatformDependentAnalyzerServices
                    get() = JvmPlatformAnalyzerServices

                override fun dependencies(): List<ModuleInfo> {
                    return dependencies
                }
            }

            val moduleInfo = FirJvmModuleInfo(module.getModuleName())
            val session: FirSession = FirSessionFactory.createJavaModuleBasedSession(moduleInfo, provider, scope).also {
                val dependenciesInfo = FirJvmModuleInfo(Name.special("<dependencies>"))
                moduleInfo.dependencies.add(dependenciesInfo)
                val librariesScope = ProjectScope.getLibrariesScope(project)
                FirSessionFactory.createLibrarySession(
                    dependenciesInfo, provider, librariesScope,
                    project, environment.createPackagePartProvider(librariesScope)
                )
                it.extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
                if (extendedAnalysisMode) {
                    it.registerExtendedCheckersComponent()
                }
            }
            val firProvider = (session.firProvider as FirProviderImpl)
            val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider, stubMode = false)
            val resolveTransformer = FirTotalResolveProcessor(session)
            val collector = FirDiagnosticsCollector.create(session)
            val firDiagnostics = mutableListOf<FirDiagnostic<*>>()
            val firFiles = ktFiles.map {
                val firFile = builder.buildFirFile(it)
                firProvider.recordFile(firFile)
                firFile
            }.also { firFiles ->
                try {
                    resolveTransformer.process(firFiles)
                    firFiles.forEach {
                        firDiagnostics += collector.collectDiagnostics(it)
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
            AnalyzerWithCompilerReport.reportDiagnostics(
                SimpleDiagnostics(
                    firDiagnostics.map { it.toRegularDiagnostic() }
                ),
                environment.messageCollector
            )
            performanceManager?.notifyAnalysisFinished()

            if (firDiagnostics.any { it.severity == Severity.ERROR }) {
                return false
            }

            performanceManager?.notifyGenerationStarted()
            val signaturer = IdSignatureDescriptor(JvmManglerDesc())

            performanceManager?.notifyIRTranslationStarted()
            val (moduleFragment, symbolTable, sourceManager, components) =
                Fir2IrConverter.createModuleFragment(
                    session, resolveTransformer.scopeSession, firFiles,
                    moduleConfiguration.languageVersionSettings, signaturer,
                    JvmGeneratorExtensions(), FirJvmKotlinMangler(session), IrFactoryImpl,
                    FirJvmVisibilityConverter
                )

            performanceManager?.notifyIRTranslationFinished()

            val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

            val codegenFactory = JvmIrCodegenFactory(moduleConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases))
            val generationState = GenerationState.Builder(
                environment.project, ClassBuilderFactories.BINARIES,
                moduleFragment.descriptor, dummyBindingContext, ktFiles,
                moduleConfiguration
            ).codegenFactory(
                codegenFactory
            ).withModule(
                module
            ).onIndependentPartCompilationEnd(
                createOutputFilesFlushingCallbackIfPossible(moduleConfiguration)
            ).isIrBackend(
                true
            ).jvmBackendClassResolver(
                FirJvmBackendClassResolver(components)
            ).build()

            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            performanceManager?.notifyIRGenerationStarted()
            generationState.beforeCompile()
            codegenFactory.generateModuleInFrontendIRMode(
                generationState, moduleFragment, symbolTable, sourceManager
            ) { irClass, context, parentFunction ->
                FirJvmClassCodegen(irClass, context, parentFunction, session)
            }
            CodegenFactory.doCheckCancelled(generationState)
            generationState.factory.done()

            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            AnalyzerWithCompilerReport.reportDiagnostics(
                FilteredJvmDiagnostics(
                    generationState.collectedExtraJvmDiagnostics,
                    dummyBindingContext.diagnostics
                ),
                environment.messageCollector
            )

            AnalyzerWithCompilerReport.reportBytecodeVersionErrors(
                generationState.extraJvmDiagnosticsTrace.bindingContext, environment.messageCollector
            )

            performanceManager?.notifyIRGenerationFinished()
            performanceManager?.notifyGenerationFinished()
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            outputs[module] = generationState
        }
        return writeOutputs(environment, projectConfiguration, chunk, outputs)
    }

    private fun FirDiagnostic<*>.toRegularDiagnostic(): Diagnostic {
        val psiSource = element as FirPsiSourceElement<*>
        @Suppress("TYPE_MISMATCH")
        when (this) {
            is FirSimpleDiagnostic ->
                return SimpleDiagnostic(
                    psiSource.psi, factory.psiDiagnosticFactory, severity
                )
            is FirDiagnosticWithParameters1<*, *> ->
                return DiagnosticWithParameters1(
                    psiSource.psi, this.a, factory.psiDiagnosticFactory, severity
                )
            is FirDiagnosticWithParameters2<*, *, *> ->
                return DiagnosticWithParameters2(
                    psiSource.psi, this.a, this.b, factory.psiDiagnosticFactory, severity
                )
            is FirDiagnosticWithParameters3<*, *, *, *> ->
                return DiagnosticWithParameters3(
                    psiSource.psi, this.a, this.b, this.c, factory.psiDiagnosticFactory, severity
                )
        }
    }

    private fun getBuildFilePaths(buildFile: File?, sourceFilePaths: List<String>): List<String> =
        if (buildFile == null) sourceFilePaths
        else sourceFilePaths.map { path ->
            (File(path).takeIf(File::isAbsolute) ?: buildFile.resolveSibling(path)).absolutePath
        }

    class MainClassProvider(generationState: GenerationState, environment: KotlinCoreEnvironment) {
        val mainClassFqName: FqName? by lazy { findMainClass(generationState, environment.getSourceFiles()) }

        private fun findMainClass(generationState: GenerationState, files: List<KtFile>): FqName? {
            val mainFunctionDetector = MainFunctionDetector(generationState.bindingContext, generationState.languageVersionSettings)
            return files.asSequence()
                .map { file ->
                    if (mainFunctionDetector.hasMain(file.declarations))
                        JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName
                    else
                        null
                }
                .singleOrNull { it != null }
        }
    }

    fun compileBunchOfSources(environment: KotlinCoreEnvironment): Boolean {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(environment.project)

        val friendPaths = environment.configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
        for (path in friendPaths) {
            moduleVisibilityManager.addFriendPath(path)
        }

        if (!checkKotlinPackageUsage(environment, environment.getSourceFiles())) return false

        val generationState = analyzeAndGenerate(environment) ?: return false

        try {
            writeOutput(environment.configuration, generationState.factory, MainClassProvider(generationState, environment))
            return true
        } finally {
            generationState.destroy()
        }
    }

    private fun repeatAnalysisIfNeeded(result: AnalysisResult?, environment: KotlinCoreEnvironment): AnalysisResult? {
        if (result is AnalysisResult.RetryWithAdditionalRoots) {
            val configuration = environment.configuration

            val oldReadOnlyValue = configuration.isReadOnly
            configuration.isReadOnly = false
            configuration.addJavaSourceRoots(result.additionalJavaRoots)
            configuration.isReadOnly = oldReadOnlyValue

            if (result.addToEnvironment) {
                environment.updateClasspath(result.additionalJavaRoots.map { JavaSourceRoot(it, null) })
            }

            if (result.additionalKotlinRoots.isNotEmpty()) {
                environment.addKotlinSourceRoots(result.additionalKotlinRoots)
            }

            KotlinJavaPsiFacade.getInstance(environment.project).clearPackageCaches()

            // Clear all diagnostic messages
            configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]?.clear()

            // Repeat analysis with additional Java roots (kapt generated sources)
            return analyze(environment)
        }

        return result
    }

    @Suppress("MemberVisibilityCanBePrivate") // Used in ExecuteKotlinScriptMojo
    fun analyzeAndGenerate(environment: KotlinCoreEnvironment): GenerationState? {
        val result = repeatAnalysisIfNeeded(analyze(environment), environment) ?: return null

        if (!result.shouldGenerateCode) return null

        result.throwIfError()

        return generate(environment, environment.configuration, result, environment.getSourceFiles(), null)
    }

    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
        val sourceFiles = environment.getSourceFiles()
        val collector = environment.messageCollector

        // Can be null for Scripts/REPL
        val performanceManager = environment.configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager?.notifyAnalysisStarted()

        val resolvedKlibs = environment.configuration.get(JVMConfigurationKeys.KLIB_PATHS)?.let { klibPaths ->
            jvmResolveLibraries(klibPaths, collector.toLogger())
        }?.getFullList() ?: emptyList()

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(collector, environment.configuration.languageVersionSettings)
        analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
            val project = environment.project
            val moduleOutputs = environment.configuration.get(JVMConfigurationKeys.MODULES)?.mapNotNullTo(hashSetOf()) { module ->
                environment.findLocalFile(module.getOutputDirectory())
            }.orEmpty()
            val sourcesOnly = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            // To support partial and incremental compilation, we add the scope which contains binaries from output directories
            // of the compiled modules (.class) to the list of scopes of the source module
            val scope = if (moduleOutputs.isEmpty()) sourcesOnly else sourcesOnly.uniteWith(DirectoriesScope(project, moduleOutputs))
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                sourceFiles,
                NoScopeRecordCliBindingTrace(),
                environment.configuration,
                environment::createPackagePartProvider,
                sourceModuleSearchScope = scope,
                klibList = resolvedKlibs
            )
        }

        performanceManager?.notifyAnalysisFinished()

        val analysisResult = analyzerWithCompilerReport.analysisResult

        return if (!analyzerWithCompilerReport.hasErrors() || analysisResult is AnalysisResult.RetryWithAdditionalRoots)
            analysisResult
        else
            null
    }

    class DirectoriesScope(
        project: Project,
        private val directories: Set<VirtualFile>
    ) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
        private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

        override fun contains(file: VirtualFile): Boolean {
            if (file.fileSystem !in fileSystems) return false

            var parent: VirtualFile = file
            while (true) {
                if (parent in directories) return true
                parent = parent.parent ?: return false
            }
        }

        override fun toString() = "All files under: $directories"
    }

    private fun GenerationState.Builder.withModule(module: Module?) =
        apply {
            if (module != null) {
                targetId(TargetId(module))
                moduleName(module.getModuleName())
                outDirectory(File(module.getOutputDirectory()))
            }
        }

    private fun generate(
        environment: KotlinCoreEnvironment,
        configuration: CompilerConfiguration,
        result: AnalysisResult,
        sourceFiles: List<KtFile>,
        module: Module?
    ): GenerationState {
        // The IR backend does not handle .kts files yet.
        var isIR = (configuration.getBoolean(JVMConfigurationKeys.IR) ||
                configuration.getBoolean(CommonConfigurationKeys.USE_FIR))
        val anyKts = sourceFiles.any { it.isScript() }
        if (isIR && anyKts) {
            environment.messageCollector.report(
                STRONG_WARNING,
                "IR backend does not support .kts scripts, switching to old JVM backend"
            )
            isIR = false
        }
        val generationState = GenerationState.Builder(
            environment.project,
            ClassBuilderFactories.BINARIES,
            result.moduleDescriptor,
            result.bindingContext,
            sourceFiles,
            configuration
        )
            .codegenFactory(
                if (isIR) JvmIrCodegenFactory(
                    configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases)
                ) else DefaultCodegenFactory
            )
            .withModule(module)
            .onIndependentPartCompilationEnd(createOutputFilesFlushingCallbackIfPossible(configuration))
            .isIrBackend(isIR)
            .build()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val performanceManager = environment.configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager?.notifyGenerationStarted()

        KotlinCodegenFacade.compileCorrectFiles(generationState)

        performanceManager?.notifyGenerationFinished()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        AnalyzerWithCompilerReport.reportDiagnostics(
            FilteredJvmDiagnostics(
                generationState.collectedExtraJvmDiagnostics,
                result.bindingContext.diagnostics
            ),
            environment.messageCollector
        )

        AnalyzerWithCompilerReport.reportBytecodeVersionErrors(
            generationState.extraJvmDiagnosticsTrace.bindingContext, environment.messageCollector
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return generationState
    }

    private val KotlinCoreEnvironment.messageCollector: MessageCollector
        get() = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
}
