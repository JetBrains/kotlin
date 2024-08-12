/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.messages.toLogger
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.metadata.AbstractMetadataSerializer
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.*
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

class FirBuiltInsSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
) : AbstractMetadataSerializer<List<ModuleCompilerAnalyzedOutput>>(configuration, environment) {
    companion object {
        fun analyzeAndSerialize(
            destDir: File,
            srcDirs: List<File>,
            extraClassPath: List<File>,
            onComplete: (totalSize: Int, totalFiles: Int) -> Unit,
        ) {
            val rootDisposable = Disposer.newDisposable("Disposable for ${FirBuiltInsSerializer::class.simpleName}.analyzeAndSerialize")
            val messageCollector = createMessageCollector()
            val performanceManager = object : CommonCompilerPerformanceManager(presentableName = "test") {}
            try {
                val configuration = CompilerConfiguration().apply {
                    this.messageCollector = messageCollector

                    addKotlinSourceRoots(srcDirs.map { it.path })
                    addJvmClasspathRoots(extraClassPath)
                    configureJdkClasspathRoots()

                    put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, destDir)
                    put(CommonConfigurationKeys.MODULE_NAME, "module for built-ins serialization")
                    put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
                }

                val environment =
                    KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

                val serializer = FirBuiltInsSerializer(configuration, environment)
                serializer.analyzeAndSerialize()

                onComplete(serializer.totalSize, serializer.totalFiles)
            } finally {
                messageCollector.flush()
                Disposer.dispose(rootDisposable)
            }
        }

        private fun createMessageCollector() = object : GroupingMessageCollector(
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false),
            false,
            false,
        ) {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                // Only report diagnostics without a particular location because there's plenty of errors in built-in sources
                // (functions without bodies, incorrect combination of modifiers, etc.)
                if (location == null) {
                    super.report(severity, message, location)
                }
            }
        }
    }

    protected var totalSize = 0
    protected var totalFiles = 0

    override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File) {
        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            throw AssertionError("Could not make directories: " + destDir)
        }

        for (output in analysisResult) {
            val (session, scopeSession, fir) = output

            val languageVersionSettings = environment.configuration.languageVersionSettings
            val firFile = fir.single()
            val packageFragment = serializeSingleFirFile(
                firFile,
                session,
                scopeSession,
                actualizedExpectDeclarations = null,
                object : FirSerializerExtensionBase(BuiltInSerializerProtocol) {
                    override val session: FirSession
                        get() = session
                    override val scopeSession: ScopeSession
                        get() = scopeSession
                    override val metadataVersion: BinaryVersion
                        get() = this@FirBuiltInsSerializer.metadataVersion
                    override val constValueProvider: ConstValueProvider?
                        get() = null
                    override val additionalMetadataProvider: FirAdditionalMetadataProvider?
                        get() = null
                },
                languageVersionSettings,
            )
            serializeBuiltInsFile(packageFragment, metadataVersion, destDir, firFile.packageFqName)
        }
    }

    private fun serializeBuiltInsFile(proto: ProtoBuf.PackageFragment, version: BuiltInsBinaryVersion, destDir: File, fqName: FqName) {
        val stream = ByteArrayOutputStream()
        with(DataOutputStream(stream)) {
            val versionArray = version.toArray()
            writeInt(versionArray.size)
            versionArray.forEach { writeInt(it) }
        }
        proto.writeTo(stream)
        write(stream, destDir, fqName)
    }

    private fun write(stream: ByteArrayOutputStream, destDir: File, fqName: FqName) {
        val destFile = File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(fqName))
        totalSize += stream.size()
        totalFiles++
        assert(!destFile.isDirectory) { "Cannot write because output destination is a directory: $destFile" }
        destFile.parentFile.mkdirs()
        destFile.writeBytes(stream.toByteArray())
    }

    private class FirJvmElementAwareStringTableForLightClasses : JvmStringTable(), FirElementAwareStringTable {
        override fun getLocalClassIdReplacement(firClass: FirClass): ClassId {
            return firClass.classId
        }
    }

    override fun analyze(): List<ModuleCompilerAnalyzedOutput>? {
        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager.notifyAnalysisStarted()

        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val rootModuleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")
        val isLightTree = configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)

        val binaryModuleData = BinaryModuleData.initialize(
            rootModuleName,
            CommonPlatforms.defaultCommonPlatform,
        )
        val libraryList = DependencyListForCliModule.build(binaryModuleData) {
            val refinedPaths = configuration.get(K2MetadataConfigurationKeys.REFINES_PATHS)?.map { File(it) }.orEmpty()
            dependencies(configuration.jvmClasspathRoots.filter { it !in refinedPaths }.map { it.toPath() })
            dependencies(configuration.jvmModularRoots.map { it.toPath() })
            friendDependencies(configuration[K2MetadataConfigurationKeys.FRIEND_PATHS] ?: emptyList())
            dependsOnDependencies(refinedPaths.map { it.toPath() })
        }

        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val klibFiles = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS).orEmpty()
            .filterIsInstance<JvmClasspathRoot>()
            .filter { it.file.isDirectory || it.file.extension == "klib" }
            .map { it.file.absolutePath }

        val logger = messageCollector.toLogger()

        // TODO: This is a workaround for KT-63573. Revert it back when KT-64169 is fixed.
//        val resolvedLibraries = CommonKLibResolver.resolve(klibFiles, logger).getFullResolvedList()
        val resolvedLibraries =
            klibFiles.map { KotlinResolvedLibraryImpl(resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File(it), logger)) }

        val outputs = if (isLightTree) {
            val projectEnvironment = environment.toAbstractProjectEnvironment() as VfsBasedProjectEnvironment
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val groupedSources = collectSources(configuration, projectEnvironment, messageCollector)
            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
            val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()
            val incrementalCompilationScope = createIncrementalCompilationScope(
                configuration,
                projectEnvironment,
                incrementalExcludesScope = null
            )?.also { librariesScope -= it }
            val sessionsWithSources = prepareCommonSessions(
                ltFiles, configuration, projectEnvironment, rootModuleName, extensionRegistrars, librariesScope,
                libraryList, resolvedLibraries, groupedSources.isCommonSourceForLt, groupedSources.fileBelongsToModuleForLt,
                createProviderAndScopeForIncrementalCompilation = { files ->
                    createContextForIncrementalCompilation(
                        configuration,
                        projectEnvironment,
                        projectEnvironment.getSearchScopeBySourceFiles(files),
                        previousStepsSymbolProviders = emptyList(),
                        incrementalCompilationScope
                    )
                }
            )
            sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirViaLightTree(files, diagnosticsReporter, performanceManager::addSourcesStats)
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }
        } else {
            val projectEnvironment = VfsBasedProjectEnvironment(
                environment.project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            ) { environment.createPackagePartProvider(it) }
            var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
            val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)
            val psiFiles = environment.getSourceFiles()
            val sourceScope =
                projectEnvironment.getSearchScopeByPsiFiles(psiFiles) + projectEnvironment.getSearchScopeForProjectJavaSources()
            val providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(
                projectEnvironment,
                configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
                configuration,
                configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId),
                sourceScope
            )
            providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
                librariesScope -= it
            }
            val sessionsWithSources = prepareCommonSessions(
                psiFiles, configuration, projectEnvironment, rootModuleName, extensionRegistrars,
                librariesScope, libraryList, resolvedLibraries, isCommonSourceForPsi, fileBelongsToModuleForPsi,
                createProviderAndScopeForIncrementalCompilation = { providerAndScopeForIncrementalCompilation }
            )

            sessionsWithSources.map { (session, files) ->
                val firFiles = session.buildFirFromKtFiles(files)
                resolveAndCheckFir(session, firFiles, diagnosticsReporter)
            }
        }

        outputs.runPlatformCheckers(diagnosticsReporter)

        val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

        return if (diagnosticsReporter.hasErrors) {
            null
        } else {
            outputs
        }.also {
            performanceManager.notifyAnalysisFinished()
        }
    }

}

@OptIn(SymbolInternals::class)
fun serializeSingleFirFile(
    file: FirFile, session: FirSession, scopeSession: ScopeSession,
    actualizedExpectDeclarations: Set<FirDeclaration>?,
    serializerExtension: FirSerializerExtension,
    languageVersionSettings: LanguageVersionSettings,
): ProtoBuf.PackageFragment {
    val approximator = TypeApproximatorForMetadataSerializer(session)

    val packageSerializer = FirElementSerializer.createTopLevel(
        session, scopeSession, serializerExtension,
        approximator,
        languageVersionSettings,
        produceHeaderKlib = false
    )
    val packageProto = packageSerializer.packagePartProto(file, actualizedExpectDeclarations).build()

    val classesProto = mutableListOf<Pair<ProtoBuf.Class, Int>>()

    fun FirClass.makeClassProtoWithNested() {
        if (!isNotExpectOrShouldBeSerialized(actualizedExpectDeclarations) ||
            !isNotPrivateOrShouldBeSerialized(produceHeaderKlib = false)
        ) {
            return
        }

        val classSerializer = FirElementSerializer.create(
            session, scopeSession, klass = this, serializerExtension, parentSerializer = null,
            approximator, languageVersionSettings, produceHeaderKlib = false
        )
        val index = classSerializer.stringTable.getFqNameIndex(this)

        classesProto += classSerializer.classProto(this).build() to index

        for (nestedClassifierSymbol in classSerializer.computeNestedClassifiersForClass(symbol)) {
            (nestedClassifierSymbol as? FirClassSymbol<*>)?.fir?.makeClassProtoWithNested()
        }
    }

    serializerExtension.processFile(file) {
        for (declaration in file.declarations) {
            (declaration as? FirClass)?.makeClassProtoWithNested()
        }
    }

    return buildPackageFragment(
        packageProto,
        classesProto,
        serializerExtension.stringTable as SerializableStringTable
    )
}

fun buildPackageFragment(
    packageProto: ProtoBuf.Package,
    classesProto: List<Pair<ProtoBuf.Class, Int>>,
    stringTable: SerializableStringTable,
): ProtoBuf.PackageFragment {

    val (stringTableProto, nameTableProto) = stringTable.buildProto()

    return ProtoBuf.PackageFragment.newBuilder()
        .setPackage(packageProto)
        .addAllClass_(classesProto.map { it.first })
        .setStrings(stringTableProto)
        .setQualifiedNames(nameTableProto)
        .build()
}

