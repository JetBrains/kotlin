/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import com.intellij.openapi.Disposable
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.cli.jvm.compiler.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.configureJdkHomeFromSystemProperty
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initialize
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.jklib.JKlibDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jklib.JKlibIrLinker
import org.jetbrains.kotlin.ir.backend.jklib.JKlibModuleSerializer
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibCliPipeline
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.JavaFlexibleTypeDeserializer
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.multiplatform.OptionalAnnotationPackageFragmentProvider
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.util.*

/**
 * This class is the entry-point for compiling Kotlin code into a Klib with references to jars.
 *
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class K2JKlibCompiler : CLICompiler<K2JKlibCompilerArguments>() {
    private lateinit var klibFactories: KlibMetadataFactories
    private lateinit var storageManager: StorageManager
    private var runtimeModule: ModuleDescriptor? = null
    private lateinit var configuration: CompilerConfiguration
    private val _descriptors: MutableMap<KotlinLibrary, ModuleDescriptorImpl> = mutableMapOf()
    override val platform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override fun createArguments() = K2JKlibCompilerArguments()

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JKlibCompilerArguments,
        services: Services,
    ) {
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JKlibCompilerArguments) {}

    enum class OutputKind {
        LIBRARY, IR,
    }

    public override fun doExecute(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode {
        this.configuration = configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val destination = File(
            arguments.destination.let {
                if (it.isNullOrBlank()) {
                    messageCollector.report(ERROR, "Specify destination via -d")
                    return ExitCode.INTERNAL_ERROR
                }
                it
            })
        val outputKind = OutputKind.valueOf((arguments.produce ?: "ir").uppercase())
        val exitCodeKlib = compileLibrary(arguments, rootDisposable, paths, destination)
        if (outputKind == OutputKind.LIBRARY || exitCodeKlib != ExitCode.OK) return exitCodeKlib
        return ExitCode.OK
    }

    private fun compileLibraryInPipeline(
        arguments: K2JKlibCompilerArguments,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
        destination: File
    ): ExitCode {
        val performanceManager = configuration.perfManager
        val pipeline = JKlibCliPipeline(performanceManager ?: object : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "JKlib") {})
        return pipeline.execute(arguments, Services.EMPTY, configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY))
    }

    fun createJarDependenciesModuleDescriptor(
        projectEnvironment: VfsBasedProjectEnvironment,
        projectContext: ProjectContext,
    ): ModuleDescriptorImpl {
        val languageVersionSettings = configuration.languageVersionSettings
        val fallbackBuiltIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FALLBACK).apply {
            initialize(builtInsModule, languageVersionSettings)
        }

        val platform = JvmPlatforms.defaultJvmPlatform
        val dependenciesContext = ContextForNewModule(
            projectContext,
            Name.special("<dependencies of ${configuration.getNotNull(MODULE_NAME)}>"),
            fallbackBuiltIns,
            platform,
        )

        // Scope for the dependency module contains everything except files present in the scope for the
        // source module
        val scope = AllJavaSourcesInProjectScope(projectContext.project)
        val dependencyScope = GlobalSearchScope.notScope(scope)



        val moduleClassResolver = SourceOrBinaryModuleClassResolver(scope)
        val lookupTracker = LookupTracker.DO_NOTHING
        val expectActualTracker = ExpectActualTracker.DoNothing
        val inlineConstTracker = InlineConstTracker.DoNothing
        val enumWhenTracker = EnumWhenTracker.DoNothing

        val configureJavaClassFinder = null
        val implicitsResolutionFilter = null
        val packagePartProvider = projectEnvironment.getPackagePartProvider(dependencyScope.toAbstractProjectFileSearchScope())
        val trace = NoScopeRecordCliBindingTrace(projectContext.project)
        val dependenciesContainer = createContainerForLazyResolveWithJava(
            platform,
            dependenciesContext,
            trace,
            DeclarationProviderFactory.EMPTY,
            dependencyScope,
            moduleClassResolver,
            CompilerEnvironment,
            lookupTracker,
            expectActualTracker,
            inlineConstTracker,
            enumWhenTracker,
            packagePartProvider,
            languageVersionSettings,
            useBuiltInsProvider = true,
            configureJavaClassFinder = configureJavaClassFinder,
            implicitsResolutionFilter = implicitsResolutionFilter,
        )
        moduleClassResolver.compiledCodeResolver = dependenciesContainer.get()

        dependenciesContext.setDependencies(
            listOf(dependenciesContext.module, fallbackBuiltIns.builtInsModule)
        )
        dependenciesContext.initializeModuleContents(
            CompositePackageFragmentProvider(
                listOf(
                    moduleClassResolver.compiledCodeResolver.packageFragmentProvider,
                    dependenciesContainer.get<JvmBuiltInsPackageFragmentProvider>(),
                    dependenciesContainer.get<OptionalAnnotationPackageFragmentProvider>(),
                ),
                "CompositeProvider@TopDownAnalyzerForJvm for dependencies ${dependenciesContext.module}",
            )
        )
        return dependenciesContext.module
    }

    @Suppress("UNUSED")
    class CompilationResult(
        val pluginContext: IrPluginContext,
        val mainModuleFragment: IrModuleFragment,
    )

    @Suppress("UNUSED")
    fun compileKlibAndDeserializeIr(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): CompilationResult {
        this.configuration = configuration
        val klibDestination = File(System.getProperty("java.io.tmpdir"), "${UUID.randomUUID()}.klib").also {
            require(!it.exists) { "Collision writing intermediate KLib $it" }
            it.deleteOnExit()
        }
        compileLibrary(arguments, rootDisposable, paths, klibDestination)
        return compileIr(arguments, rootDisposable, klibDestination)
    }

    fun compileIr(
        arguments: K2JKlibCompilerArguments,
        disposable: Disposable,
        klib: File,
    ): CompilationResult {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        configuration.put(MODULE_NAME, arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

        val projectEnvironment = createProjectEnvironment(
            configuration,
            disposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
            messageCollector,
        )

        val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS) + klib.absolutePath

        val projectContext = ProjectContext(projectEnvironment.project, "TopDownAnalyzer for JKlib")
        storageManager = projectContext.storageManager
        val builtIns = JvmBuiltIns(projectContext.storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)

        klibFactories = KlibMetadataFactories({ builtIns }, JavaFlexibleTypeDeserializer)
        val trace = BindingTraceContext(projectContext.project)

        val allDependencies = CommonKLibResolver.resolve(klibFiles, configuration.getLogger())
        val moduleDependencies =
            allDependencies.getFullResolvedList().associate { klib -> klib.library to klib.resolvedDependencies.map { d -> d.library } }
                .toMap()
        val sortedDependencies = sortDependencies(moduleDependencies)

        val jarDepsModuleDescriptor = createJarDependenciesModuleDescriptor(projectEnvironment, projectContext)
        val descriptors = sortedDependencies.map { getModuleDescriptor(it) } + jarDepsModuleDescriptor
        //descriptors.forEach { it.setDependencies(descriptors) }
        descriptors.forEach { if (it != jarDepsModuleDescriptor) it.setDependencies(descriptors) }

        val mainModuleLib = sortedDependencies.find { it.libraryFile == klib }

        val mainModule = getModuleDescriptor(mainModuleLib!!)

        val mangler = JKlibDescriptorMangler(
            MainFunctionDetector(trace.bindingContext, configuration.languageVersionSettings)
        )
        val symbolTable = SymbolTable(IdSignatureDescriptor(mangler), IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, mainModule)
        val irBuiltIns = IrBuiltInsOverDescriptors(mainModule.builtIns, typeTranslator, symbolTable)

        val stubGenerator = DeclarationStubGeneratorImpl(
            mainModule,
            symbolTable,
            irBuiltIns,
            DescriptorByIdSignatureFinderImpl(mainModule, mangler),
            JvmGeneratorExtensionsImpl(configuration),
        ).apply { unboundSymbolGeneration = true }
        val linker = JKlibIrLinker(
            module = mainModule,
            messageCollector = messageCollector,
            irBuiltIns = irBuiltIns,
            symbolTable = symbolTable,
            stubGenerator = stubGenerator,
            mangler = mangler,
        )

        val pluginContext = IrPluginContextImpl(
            mainModule,
            trace.bindingContext,
            configuration.languageVersionSettings,
            symbolTable,
            typeTranslator,
            irBuiltIns,
            linker = linker,
            messageCollector = messageCollector,
        )

        // Deserialize modules
        // We explicitly use the DeserializationStrategy.ALL to deserialize the whole world,
        // so that we don't rely on linker side effects for proper deserialization.
        linker.deserializeIrModuleHeader(
            jarDepsModuleDescriptor,
            null,
            { DeserializationStrategy.ALL },
            jarDepsModuleDescriptor.name.asString(),
        )

        lateinit var mainModuleFragment: IrModuleFragment
        for (dep in sortedDependencies) {
            val descriptor = getModuleDescriptor(dep)
            when {
                descriptor == mainModule -> {
                    mainModuleFragment = linker.deserializeIrModuleHeader(descriptor, dep, { DeserializationStrategy.ALL })
                }
                else -> linker.deserializeIrModuleHeader(descriptor, dep, { DeserializationStrategy.ALL })
            }
        }

        irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(irBuiltIns, symbolTable, typeTranslator, null, true)

        linker.init(null)
        ExternalDependenciesGenerator(symbolTable, listOf(linker)).generateUnboundSymbolsAsDependencies()
        linker.postProcess(inOrAfterLinkageStep = true)

        linker.checkNoUnboundSymbols(symbolTable, "Found unbound symbol")

        return CompilationResult(pluginContext, mainModuleFragment)
    }

    private fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl {
        if (current in _descriptors) {
            return _descriptors.getValue(current)
        }

        val isBuiltIns = current.unresolvedDependencies.isEmpty()

        val lookupTracker = LookupTracker.DO_NOTHING
        val md = klibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            configuration.languageVersionSettings,
            storageManager,
            runtimeModule?.builtIns,
            packageAccessHandler = null,
            lookupTracker = lookupTracker,
        )
        if (isBuiltIns) runtimeModule = md

        _descriptors[current] = md

        return md
    }

    private fun compileLibrary(
        arguments: K2JKlibCompilerArguments,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
        destination: File,
    ): ExitCode {
        val collector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val performanceManager = configuration.perfManager

        val pluginLoadResult = loadPlugins(paths, arguments, configuration, rootDisposable)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val commonSources = arguments.commonSources?.toSet() ?: emptySet()

        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, isCommon = arg in commonSources, hmppModuleName = null)
        }

        with(configuration) {
            if (arguments.noJdk) {
                put(JVMConfigurationKeys.NO_JDK, true)
            } else {
                configureJdkHomeFromSystemProperty()
            }
            configuration.configureJdkClasspathRoots()
            if (!arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::stdlibPath,
                    PathUtil.KOTLIN_JAVA_STDLIB_JAR,
                    messageCollector,
                    "'-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.stdlib")
                }
                getLibraryFromHome(
                    paths,
                    KotlinPaths::scriptRuntimePath,
                    PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR,
                    messageCollector,
                    "'-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.script.runtime")
                }
            }
            // "-no-stdlib" implies "-no-reflect": otherwise we would be able to transitively read stdlib
            // classes through kotlin-reflect,
            // which is likely not what user wants since s/he manually provided "-no-stdlib"`
            if (!arguments.noReflect && !arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::reflectPath,
                    PathUtil.KOTLIN_JAVA_REFLECT_JAR,
                    messageCollector,
                    "'-no-reflect' or '-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.reflect")
                }
            }
            arguments.klibLibraries?.let { libraries ->
                put(
                    JVMConfigurationKeys.KLIB_PATHS,
                    libraries.split(File.pathSeparator.toRegex()).filterNot(String::isEmpty),
                )
            }
            for (path in arguments.classpath?.split(java.io.File.pathSeparatorChar).orEmpty()) {
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(java.io.File(path)))
            }
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(MODULE_NAME, moduleName)

        configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configuration.put(
            CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME,
            arguments.renderInternalDiagnosticNames,
        )

        val projectEnvironment = createProjectEnvironment(
            configuration,
            rootDisposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
            collector,
        )
        val groupedSources = collectSources(configuration, projectEnvironment, collector)

        if (groupedSources.isEmpty()) {
            if (arguments.version) {
                return ExitCode.OK
            }
            collector.report(ERROR, "No source files")
            return COMPILATION_ERROR
        }

        try {
            val rootModuleNameAsString = configuration.getNotNull(MODULE_NAME)
            val rootModuleName = Name.special("<${rootModuleNameAsString}>")

            val module = ModuleBuilder(
                configuration[MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
                destination.path,
                "java-production",
            )
            with(module) {
                arguments.friendPaths?.forEach { addFriendDir(it) }
                arguments.classpath?.split(File.pathSeparator)?.forEach { addClasspathEntry(it) }
            }

            val diagnosticsReporter = DiagnosticsCollectorImpl()

            val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS)

            val resolvedLibraries = klibFiles.map { KotlinResolvedLibraryImpl(resolveSingleFileKlib(File(it), collector)) }
            val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
            val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()

            val libraryList = DependencyListForCliModule.build(Name.identifier(moduleName)) {
                dependencies(configuration.jvmClasspathRoots.map { it.absolutePath })
                dependencies(configuration.jvmModularRoots.map { it.absolutePath })
                dependencies(resolvedLibraries.map { it.library.libraryFile.absolutePath })
                friendDependencies(arguments.friendModules?.split(File.pathSeparator) ?: emptyList())
            }

            val librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

            val sessionsWithSources = prepareJKlibSessions(
                projectEnvironment,
                ltFiles,
                configuration,
                rootModuleName,
                resolvedLibraries,
                libraryList,
                extensionRegistrars,
                metadataCompilationMode = false,
                isCommonSource = groupedSources.isCommonSourceForLt,
                fileBelongsToModule = groupedSources.fileBelongsToModuleForLt,
                librariesScope = librariesScope,
            )
            val outputs = sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirViaLightTree(
                    files,
                    diagnosticsReporter,
                    if (performanceManager != null) performanceManager::addSourcesStats else null,
                )
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }

            outputs.runPlatformCheckers(diagnosticsReporter)

            val firFiles = outputs.flatMap { it.fir }
            checkKotlinPackageUsageForLightTree(configuration, firFiles)

            val renderDiagnosticName = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            if (diagnosticsReporter.hasErrors) {
                diagnosticsReporter.reportToMessageCollector(collector, renderDiagnosticName)
                return COMPILATION_ERROR
            }

            val firResult = AllModulesFrontendOutput(outputs)

            val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
            val irGenerationExtensions = configuration.getCompilerExtensions(IrGenerationExtension)
            val fir2IrResult = firResult.convertToIrAndActualizeForJvm(
                fir2IrExtensions,
                configuration,
                diagnosticsReporter,
                irGenerationExtensions,
            )

            val produceHeaderKlib = true

            val serializerOutput = serializeModuleIntoKlib(
                moduleName = fir2IrResult.irModuleFragment.name.asString(),
                irModuleFragment = fir2IrResult.irModuleFragment,
                configuration = configuration,
                diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
                    diagnosticsReporter.deduplicating(),
                    configuration.languageVersionSettings
                ),
                cleanFiles = emptyList(),
                dependencies = resolvedLibraries.map { it.library },
                createModuleSerializer = { irDiagnosticReporter: IrDiagnosticReporter ->
                    JKlibModuleSerializer(IrSerializationSettings(configuration), irDiagnosticReporter)
                },
                metadataSerializer = Fir2KlibMetadataSerializer(
                    configuration,
                    firResult.outputs,
                    produceHeaderKlib = produceHeaderKlib,
                    fir2IrActualizedResult = fir2IrResult,
                    exportKDoc = false,
                ),
            )

            val versions = KotlinLibraryVersioning(
                abiVersion = KotlinAbiVersion.CURRENT,
                compilerVersion = KotlinCompilerVersion.getVersion(),
                metadataVersion = configuration.klibMetadataVersionOrDefault(),
            )

            KlibWriter {
                format(KlibFormat.ZipArchive)
                manifest {
                    moduleName(configuration[MODULE_NAME]!!)
                    versions(versions)
                    platformAndTargets(BuiltInsPlatform.COMMON, emptyList())
                }
                includeMetadata(serializerOutput.serializedMetadata ?: error("expected serialized metadata"))
                includeIr(serializerOutput.serializedIr)
            }.writeTo(destination.absolutePath)
        } catch (e: CompilationException) {
            collector.report(
                EXCEPTION,
                OutputMessageUtil.renderException(e),
                MessageUtil.psiElementToMessageLocation(e.element),
            )
            return ExitCode.INTERNAL_ERROR
        }

        return ExitCode.OK
    }

    private fun resolveSingleFileKlib(file: File, collector: MessageCollector): KotlinLibrary {
        val klibLoadingResult = KlibLoader { libraryPaths(file.path) }.load()
        klibLoadingResult.reportLoadingProblemsIfAny { _, message ->
            collector.report(ERROR, message)
        }
        return klibLoadingResult.librariesStdlibFirst.single()
    }

    override fun executableScriptFileName(): String = "kotlinc"

    public override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = BuiltInsBinaryVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JKlibCompiler(), args)
        }
    }
}

private class SourceOrBinaryModuleClassResolver(private val sourceScope: GlobalSearchScope) : ModuleClassResolver {
    lateinit var compiledCodeResolver: JavaDescriptorResolver
    lateinit var sourceCodeResolver: JavaDescriptorResolver

    override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
        val resolver = if (javaClass is VirtualFileBoundJavaClass && javaClass.isFromSourceCodeInScope(sourceScope)) sourceCodeResolver
        else compiledCodeResolver
        return resolver.resolveClass(javaClass)
    }
}
