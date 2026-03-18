/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import com.intellij.openapi.Disposable
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jklib.pipeline.JKLIB_OUTPUT_DESTINATION
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibCliPipeline
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initialize
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jklib.JKlibDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jklib.JKlibIrLinker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.JavaFlexibleTypeDeserializer
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
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
import org.jetbrains.kotlin.utils.KotlinPaths
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

    public override fun doExecutePhased(
        arguments: K2JKlibCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        return compileLibrary(arguments, services, basicMessageCollector)
    }

    public override fun doExecute(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode = error("K1 compiler entry point is no supported.")

    private fun compileLibrary(
        arguments: K2JKlibCompilerArguments,
        services: Services,
        messageCollector: MessageCollector,
    ): ExitCode = JKlibCliPipeline(defaultPerformanceManager).execute(arguments, services, messageCollector)


    @Suppress("UNUSED")
    class CompilationResult(
        val pluginContext: IrPluginContext,
        val mainModuleFragment: IrModuleFragment,
    )

    /** Entry point used by J2CL to get the IR tree. */
    @Suppress("UNUSED")
    fun compileKlibAndDeserializeIr(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
    ): CompilationResult {
        configuration.put(
            JKLIB_OUTPUT_DESTINATION,
            "${System.getProperty("java.io.tmpdir")}${File.pathSeparator}${UUID.randomUUID()}.klib"
        )
        compileLibrary(arguments, Services.EMPTY, configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY))
        return compileIr(configuration, rootDisposable)
    }

    // TODO(KT-84884): Move IR compilation logic in a specific PipelinePhase.
    private fun compileIr(
        configuration: CompilerConfiguration,
        disposable: Disposable,
    ): CompilationResult {
        val klib = File(configuration.getNotNull(JKLIB_OUTPUT_DESTINATION)).deleteOnExit()
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val projectEnvironment = createProjectEnvironment(
            configuration,
            disposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )

        val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS) + klib.absolutePath

        val projectContext = ProjectContext(projectEnvironment.project, "TopDownAnalyzer for JKlib")
        storageManager = projectContext.storageManager
        val builtIns = JvmBuiltIns(projectContext.storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)

        klibFactories = KlibMetadataFactories({ builtIns }, JavaFlexibleTypeDeserializer)
        val trace = BindingTraceContext(projectContext.project)

        val sortedDependencies = loadLibraries(klibFiles, messageCollector)

        val jarDepsModuleDescriptor = createJarDependenciesModuleDescriptor(configuration, projectEnvironment, projectContext)
        val descriptors = sortedDependencies.map { getModuleDescriptor(configuration, it) } + jarDepsModuleDescriptor
        descriptors.forEach { if (it != jarDepsModuleDescriptor) it.setDependencies(descriptors) }

        val mainModuleLib = sortedDependencies.find { it.libraryFile == klib }

        val mainModule = getModuleDescriptor(configuration, mainModuleLib!!)

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
            val descriptor = getModuleDescriptor(configuration, dep)
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

    private fun createJarDependenciesModuleDescriptor(
        configuration: CompilerConfiguration,
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

    private fun getModuleDescriptor(configuration: CompilerConfiguration, current: KotlinLibrary): ModuleDescriptorImpl {
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
            lookupTracker = lookupTracker,
        )
        if (isBuiltIns) runtimeModule = md

        _descriptors[current] = md

        return md
    }

    private fun loadLibraries(klibFiles: List<String>, collector: MessageCollector): List<KotlinLibrary> {
        val loadingResult = KlibLoader { libraryPaths(klibFiles) }.load()
        loadingResult.reportLoadingProblemsIfAny { _, message ->
            collector.report(ERROR, message)
        }
        return loadingResult.librariesStdlibFirst
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
