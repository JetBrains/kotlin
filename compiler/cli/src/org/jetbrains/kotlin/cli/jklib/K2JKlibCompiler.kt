/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.common.toLogger
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
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
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.configureJdkHomeFromSystemProperty
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import java.util.*

/**
 * This class is the entry-point for compiling Kotlin code into a Klib with references to jars.
 *
 */
@OptIn(UnsafeDuringIrConstructionAPI::class, ObsoleteDescriptorBasedAPI::class)
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
        configuration: CompilerConfiguration, arguments: K2JKlibCompilerArguments, services: Services,
    ) {
        // No specific arguments yet
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JKlibCompilerArguments) {}

    enum class OutputKind {
        LIBRARY,
        IR
    }

    @UnsafeDuringIrConstructionAPI
    class JKlibLinker(
        module: ModuleDescriptor,
        messageCollector: MessageCollector,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        val stubGenerator: DeclarationStubGeneratorImpl,
        val mangler: JvmDescriptorMangler,
    ) : KotlinIrLinker(module, messageCollector, irBuiltIns, symbolTable, emptyList()) {
        override val returnUnboundSymbolsIfSignatureNotFound
            get() = false

        private val javaName = Name.identifier("java")

        private fun DeclarationDescriptor.isJavaDescriptor(): Boolean {
            if (this is PackageFragmentDescriptor) {
                return this is LazyJavaPackageFragment || fqName.startsWith(javaName)
            }

            return this is JavaClassDescriptor || this is JavaCallableMemberDescriptor || (containingDeclaration?.isJavaDescriptor() == true)
        }

        override fun platformSpecificSymbol(symbol: IrSymbol): Boolean {
            return symbol.descriptor.isJavaDescriptor()
        }

        private fun DeclarationDescriptor.isCleanDescriptor(): Boolean {
            if (this is PropertyAccessorDescriptor) return correspondingProperty.isCleanDescriptor()
            return this is DeserializedDescriptor
        }

        private fun declareJavaFieldStub(symbol: IrFieldSymbol): IrField {
            return with(stubGenerator) {
                val old = stubGenerator.unboundSymbolGeneration
                try {
                    stubGenerator.unboundSymbolGeneration = true
                    generateFieldStub(symbol.descriptor)
                } finally {
                    stubGenerator.unboundSymbolGeneration = old
                }
            }
        }

        override val fakeOverrideBuilder =
            IrLinkerFakeOverrideProvider(
                linker = this,
                symbolTable = symbolTable,
                mangler = JvmIrMangler,
                typeSystem = IrTypeSystemContextImpl(builtIns),
                friendModules = emptyMap(), // TODO: provide friend modules?
                partialLinkageSupport = partialLinkageSupport,
            )

        override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
            moduleDescriptor === moduleDescriptor.builtIns.builtInsModule

        private val IrLibrary.libContainsErrorCode: Boolean
            get() = this is KotlinLibrary && this.containsErrorCode

        override fun createModuleDeserializer(
            moduleDescriptor: ModuleDescriptor,
            klib: KotlinLibrary?,
            strategyResolver: (String) -> DeserializationStrategy,
        ): IrModuleDeserializer {
            require(klib != null) { "Expecting kotlin library" }
            klib.moduleHeaderData
            val libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
            return JKlibModuleDeserializer(
                moduleDescriptor,
                klib,
                strategyResolver,
                libraryAbiVersion,
                klib.libContainsErrorCode,
                stubGenerator
            )
        }

        private val deserializedFilesInKlibOrder = mutableMapOf<IrModuleFragment, List<IrFile>>()

        private inner class JKlibModuleDeserializer(
            moduleDescriptor: ModuleDescriptor,
            klib: IrLibrary,
            strategyResolver: (String) -> DeserializationStrategy,
            libraryAbiVersion: KotlinAbiVersion,
            allowErrorCode: Boolean,
            stubGenerator: DeclarationStubGenerator,
        ) :
            BasicIrModuleDeserializer(
                this,
                moduleDescriptor,
                klib,
                strategyResolver,
                libraryAbiVersion,
                allowErrorCode,
            ) {

            override fun init(delegate: IrModuleDeserializer) {
                super.init(delegate)
                deserializedFilesInKlibOrder[moduleFragment] =
                    fileDeserializationStates.memoryOptimizedMap { it.file }
            }

            private val descriptorSignatures = mutableMapOf<DeclarationDescriptor, IdSignature>()

            private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
                moduleDescriptor, mangler,
                DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY
            )

            private val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

            override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
                super.tryDeserializeIrSymbol(idSig, symbolKind)?.let { return it }
                deserializedSymbols[idSig]?.let { return it }
                val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: return null
                descriptorSignatures[descriptor] = idSig
                return (stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner).symbol
            }
        }

        override fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer =
            JvmCurrentModuleDeserializer(moduleFragment, dependencies)

        private inner class JvmCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>) :
            CurrentModuleDeserializer(moduleFragment, dependencies) {
            override fun declareIrSymbol(symbol: IrSymbol) {
                val descriptor = symbol.descriptor

                if (descriptor.isJavaDescriptor()) {
                    // Wrap java declaration with lazy ir
                    if (symbol is IrFieldSymbol) {
                        declareJavaFieldStub(symbol)
                    } else {
                        stubGenerator.generateMemberStub(descriptor)
                    }
                    return
                }

                if (descriptor.isCleanDescriptor()) {
                    stubGenerator.generateMemberStub(descriptor)
                    return
                }

                super.declareIrSymbol(symbol)
            }
        }
    }

    public override fun doExecute(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode {
        this.configuration = configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val destination = File(arguments.destination.let {
            if (it.isNullOrBlank()) {
                messageCollector.report(ERROR, "Specify destination via -d")
                return ExitCode.INTERNAL_ERROR
            }
            it
        })
        val outputKind = OutputKind.valueOf((arguments.produce ?: "ir").uppercase())
        val klibDestination = if (outputKind == OutputKind.LIBRARY) destination else
            File(System.getProperty("java.io.tmpdir"), "${UUID.randomUUID()}.klib").also {
                require(!it.exists) { "Collision writing intermediate KLib $it" }
                it.deleteOnExit()
            }
        val exitCodeKlib = compileLibrary(arguments, rootDisposable, paths, klibDestination)
        if (outputKind == OutputKind.LIBRARY || exitCodeKlib != ExitCode.OK) return exitCodeKlib

        return compileIr(arguments, rootDisposable, paths, klibDestination)

    }

    fun compileIr(
        arguments: K2JKlibCompilerArguments,
        disposable: Disposable,
        paths: KotlinPaths?,
        klib: File,
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        configuration.put(MODULE_NAME, arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

        val projectEnvironment =
            createProjectEnvironment(
                configuration,
                disposable,
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
                messageCollector,
            )

        val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS) + klib.absolutePath
        val friendFiles = configuration.getList(JVMConfigurationKeys.FRIEND_PATHS).map { File(it) }.toSet()

        val projectContext = ProjectContext(projectEnvironment.project, "TopDownAnalyzer for JKlib")
        storageManager = projectContext.storageManager
        val builtIns = JvmBuiltIns(projectContext.storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)

        klibFactories = KlibMetadataFactories(
            { storageManager -> builtIns },
            NullFlexibleTypeDeserializer
        )
        val trace = BindingTraceContext(projectContext.project)

        val allDependencies = CommonKLibResolver.resolve(
            klibFiles,
            messageCollector.toLogger(),
        )
        val moduleDependencies = allDependencies.getFullResolvedList().associate { klib ->
            klib.library to klib.resolvedDependencies.map { d -> d.library }
        }.toMap()
        val sortedDependencies = sortDependencies(moduleDependencies)

        val descriptors = sortedDependencies.map { getModuleDescriptor(it) }
        descriptors.forEach { descriptor ->
            descriptor.setDependencies(descriptors)// + listOf(dependencyModule))
        }

        val mainModuleLib = sortedDependencies.find { it.libraryFile == klib }

        val mainModule = getModuleDescriptor(mainModuleLib!! )

        val mangler = JvmDescriptorMangler(
            MainFunctionDetector(trace.bindingContext, configuration.languageVersionSettings)
        )
        val symbolTable = SymbolTable(IdSignatureDescriptor(mangler), IrFactoryImpl)
        val translator = Psi2IrTranslator(
            configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false, false),
            messageCollector::checkNoUnboundSymbols
        )
        val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, mainModule)
        val irBuiltIns = IrBuiltInsOverDescriptors(mainModule.builtIns, typeTranslator, symbolTable)

        val stubGenerator =
            DeclarationStubGeneratorImpl(
                mainModule, symbolTable, irBuiltIns,
                DescriptorByIdSignatureFinderImpl(mainModule, mangler),
                JvmGeneratorExtensionsImpl(configuration)
            )
        val linker = JKlibLinker(
            module = mainModule,
            messageCollector = messageCollector,
            irBuiltIns = irBuiltIns,
            symbolTable = symbolTable,
            stubGenerator = stubGenerator,
            mangler = mangler,
        )

        val deserializedModuleFragmentsToLib = sortedDependencies.associateBy { klib ->
            val descriptor = getModuleDescriptor(klib)
            when {
                descriptor == mainModule -> linker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.ALL })
                else -> linker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
            }
        }

        val deserializedModuleFragments = deserializedModuleFragmentsToLib.keys.toList()
        irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
            irBuiltIns,
            symbolTable,
            typeTranslator,
            null,
            true
        )

        val moduleFragment = deserializedModuleFragments.last()

        linker.init(null)
        ExternalDependenciesGenerator(symbolTable, listOf(linker)).generateUnboundSymbolsAsDependencies()

        linker.postProcess(inOrAfterLinkageStep = true)

        println(moduleFragment.dump())

        return ExitCode.OK
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
            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
            lookupTracker = lookupTracker
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
        val performanceManager = configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)

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
                    "'-no-stdlib'"
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.stdlib")
                }
                getLibraryFromHome(
                    paths,
                    KotlinPaths::scriptRuntimePath,
                    PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR,
                    messageCollector,
                    "'-no-stdlib'"
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.script.runtime")
                }
            }
            // "-no-stdlib" implies "-no-reflect": otherwise we would be able to transitively read stdlib classes through kotlin-reflect,
            // which is likely not what user wants since s/he manually provided "-no-stdlib"`
            if (!arguments.noReflect && !arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::reflectPath,
                    PathUtil.KOTLIN_JAVA_REFLECT_JAR,
                    messageCollector,
                    "'-no-reflect' or '-no-stdlib'"
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.reflect")
                }
            }
            arguments.klibLibraries?.let { libraries ->
                put(JVMConfigurationKeys.KLIB_PATHS, libraries.split(File.pathSeparator.toRegex()).filterNot(String::isEmpty))
            }
            for (path in arguments.classpath?.split(java.io.File.pathSeparatorChar).orEmpty()) {
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(java.io.File(path)))
            }
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(MODULE_NAME, moduleName)

        configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configuration.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

        val projectEnvironment = createProjectEnvironment(
            configuration,
            rootDisposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
            collector
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
                destination.path, "java-production"
            )
            with(module) {
                arguments.friendPaths?.forEach { addFriendDir(it) }
                arguments.classpath?.split(File.pathSeparator)?.forEach { addClasspathEntry(it) }
            }

            val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(collector)

            val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS)

            val logger = collector.toLogger()

            val resolvedLibraries = klibFiles.map {
                KotlinResolvedLibraryImpl(
                    resolveSingleFileKlib(
                        File(it),
                        logger
                    )
                )
            }
            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
            val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()

            val libraryList = DependencyListForCliModule.build(Name.identifier(moduleName)) {
                dependencies(configuration.jvmClasspathRoots.map { it.absolutePath })
                dependencies(configuration.jvmModularRoots.map { it.absolutePath })
                dependencies(resolvedLibraries.map { it.library.libraryFile.absolutePath })
                friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
                friendDependencies(module.getFriendPaths())
            }

            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()


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
                val firFiles = session.buildFirViaLightTree(files, diagnosticsReporter, performanceManager::addSourcesStats)
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

            val firResult = FirResult(outputs)


            val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
            val irGenerationExtensions = IrGenerationExtension.getInstances(projectEnvironment.project)
            val fir2IrResult =
                firResult.convertToIrAndActualizeForJvm(fir2IrExtensions, configuration, diagnosticsReporter, irGenerationExtensions)

            val produceHeaderKlib = true // TODO: make CLI argument instead

            val serializerOutput = serializeModuleIntoKlib(
                moduleName = fir2IrResult.irModuleFragment.name.asString(),
                irModuleFragment = fir2IrResult.irModuleFragment,
                configuration = configuration,
                diagnosticReporter = diagnosticsReporter,
                cleanFiles = emptyList(),
                dependencies = resolvedLibraries.map { it.library },
                createModuleSerializer = { irDiagnosticReporter: IrDiagnosticReporter ->
                    JKlibModuleSerializer(
                        IrSerializationSettings(configuration),
                        irDiagnosticReporter
                    )
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

            buildKotlinLibrary(
                linkDependencies = serializerOutput.neededLibraries,
                ir = serializerOutput.serializedIr,
                metadata = serializerOutput.serializedMetadata ?: error("expected serialized metadata"),
                versions = versions,
                output = destination.absolutePath,
                moduleName = configuration[MODULE_NAME]!!,
                nopack = false,
                manifestProperties = null,
                builtInsPlatform = BuiltInsPlatform.COMMON,
                nativeTargets = emptyList()
            )
        } catch (e: CompilationException) {
            collector.report(EXCEPTION, OutputMessageUtil.renderException(e), MessageUtil.psiElementToMessageLocation(e.element))
            return ExitCode.INTERNAL_ERROR
        }

        return ExitCode.OK
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
