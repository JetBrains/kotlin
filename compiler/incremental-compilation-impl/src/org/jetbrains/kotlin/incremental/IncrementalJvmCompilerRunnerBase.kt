/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.DATA_PATH
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.buildtools.internal.cri.CriDataSerializerImpl
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade.Companion.getVisibleTypeAliasFqNames
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.SubtypeTracker
import org.jetbrains.kotlin.incremental.javaInterop.JavaInteropCoordinator
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

/**
 * Hold common logic of all JVM incremental runners
 *
 * Subclasses differ significantly based on their sourcesToCompile calculation and compile avoidance approach,
 * but it doesn't make sense to copy all this shared code into them
 * (and it's hard to break away from inheritance-based architecture)
 */
abstract class IncrementalJvmCompilerRunnerBase(
    workingDir: File,
    reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    buildHistoryFile: File?, // part of build history implementation is in abstract runner, so this stays
    outputDirs: Collection<File>?,
    kotlinSourceFilesExtensions: Set<String>,
    icFeatures: IncrementalCompilationFeatures,
    private val generateCompilerRefIndex: Boolean = false,
) : IncrementalCompilerRunner<K2JVMCompilerArguments, IncrementalJvmCachesManager>(
    workingDir,
    "caches-jvm",
    reporter,
    buildHistoryFile = buildHistoryFile,
    outputDirs = outputDirs,
    kotlinSourceFilesExtensions = kotlinSourceFilesExtensions,
    icFeatures = icFeatures,
) {
    override val shouldStoreFullFqNamesInLookupCache = true

    protected val messageCollector = MessageCollectorImpl()
    internal val javaInteropCoordinator = JavaInteropCoordinator.getImplementation(
        icFeatures.usePreciseJavaTracking,
        reporter,
        messageCollector,
    )

    private val subtypeTracker = if (generateCompilerRefIndex) SubtypeTrackerImpl() else null

    override fun createCacheManager(icContext: IncrementalCompilationContext, args: K2JVMCompilerArguments) =
        IncrementalJvmCachesManager(
            icContext = icContext,
            outputDir = args.destination?.let { File(it) },
            cachesRootDir = cacheDirectory,
            subtypeTracker = subtypeTracker ?: SubtypeTracker.DoNothing,
        )

    override fun updateCaches(
        services: Services,
        caches: IncrementalJvmCachesManager,
        generatedFiles: List<GeneratedFile>,
        changesCollector: ChangesCollector
    ) {
        updateIncrementalCache(
            generatedFiles, caches.platformCache, changesCollector,
            services[JavaClassesTracker::class.java] as? JavaClassesTrackerImpl
        )
    }

    override fun destinationDir(args: K2JVMCompilerArguments): File =
        args.destinationAsFile

    override fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
        javaInteropCoordinator.getAdditionalDirtyLookupSymbols()

    override fun additionalDirtyFiles(
        caches: IncrementalJvmCachesManager,
        generatedFiles: List<GeneratedFile>,
        services: Services
    ): Iterable<File> {
        val cache = caches.platformCache
        val result = HashSet<File>()

        fun partsByFacadeName(facadeInternalName: String): List<File> {
            val parts = cache.getStableMultifileFacadeParts(facadeInternalName) ?: emptyList()
            return parts.flatMap { cache.sourcesByInternalName(it) }
        }

        for (generatedFile in generatedFiles) {
            if (generatedFile !is GeneratedJvmClass) continue

            val outputClass = generatedFile.outputClass

            fun addSourceFilesWhenClassLikeDeclarationNameClash(classLikeFqName: FqName) {
                result.addIfNotNull(cache.getSourceFileIfClass(classLikeFqName))
                result.addAll(cache.getSourceFilesIfTypealias(classLikeFqName))
            }

            when (outputClass.classHeader.kind) {
                KotlinClassHeader.Kind.CLASS -> {
                    addSourceFilesWhenClassLikeDeclarationNameClash(outputClass.className.fqNameForClassNameWithoutDollars)
                }
                // todo: more optimal is to check if public API or parts list changed
                KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
                    result.addAll(partsByFacadeName(outputClass.className.internalName))
                }
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    result.addAll(partsByFacadeName(outputClass.classHeader.multifileClassName!!))
                }
                KotlinClassHeader.Kind.FILE_FACADE -> {
                    val packagePartProtoData = KotlinClassInfo.createFrom(generatedFile.outputClass).protoData as PackagePartProtoData
                    for (typeAliasName in packagePartProtoData.getVisibleTypeAliasFqNames()) {
                        addSourceFilesWhenClassLikeDeclarationNameClash(typeAliasName)
                    }
                }
                KotlinClassHeader.Kind.SYNTHETIC_CLASS, KotlinClassHeader.Kind.UNKNOWN -> {
                }
            }
        }

        return result
    }

    override fun runWithNoDirtyKotlinSources(caches: IncrementalJvmCachesManager): Boolean =
        caches.platformCache.getObsoleteJavaClasses().isNotEmpty() || javaInteropCoordinator.hasChangedUntrackedJavaClasses()

    override fun makeServices(
        args: K2JVMCompilerArguments,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        fileMappingTracker: ICFileMappingTracker,
        caches: IncrementalJvmCachesManager,
        dirtySources: Set<File>,
        isIncremental: Boolean
    ): Services.Builder =
        super.makeServices(args, lookupTracker, expectActualTracker, fileMappingTracker, caches, dirtySources, isIncremental).apply {
            val moduleName = requireNotNull(args.moduleName) { "'moduleName' is null!" }
            val targetId = TargetId(moduleName, "java-production")
            val targetToCache = mapOf(targetId to caches.platformCache)
            val incrementalComponents = IncrementalCompilationComponentsImpl(targetToCache)
            register(IncrementalCompilationComponents::class.java, incrementalComponents)
            javaInteropCoordinator.makeJavaClassesTracker(caches.platformCache)?.let {
                register(JavaClassesTracker::class.java, it)
            }
        }

    override fun performWorkBeforeCompilation(compilationMode: CompilationMode, args: K2JVMCompilerArguments) {
        super.performWorkBeforeCompilation(compilationMode, args)

        if (compilationMode is CompilationMode.Incremental) {
            args.classpathAsList = listOf(args.destinationAsFile) + args.classpathAsList
        }
    }

    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: K2JVMCompilerArguments,
        caches: IncrementalJvmCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean
    ): Pair<ExitCode, Collection<File>> {
        val compiler = K2JVMCompiler()
        val freeArgsBackup = args.freeArgs.toList()
        args.freeArgs += sourcesToCompile.map { it.absolutePath }
        args.allowNoSourceFiles = true
        val exitCode = compiler.exec(messageCollector, services, args)
        args.freeArgs = freeArgsBackup
        reporter.reportPerformanceData(compiler.defaultPerformanceManager.unitStats)
        return exitCode to sourcesToCompile
    }

    @OptIn(ExperimentalBuildToolsApi::class)
    override fun generateCompilerRefIndexIfNeeded(
        services: Services,
        sourceFilesPathConverter: FileToPathConverter,
        compilationMode: CompilationMode,
    ) {
        super.generateCompilerRefIndexIfNeeded(services, sourceFilesPathConverter, compilationMode)
        if (!generateCompilerRefIndex) return

        reporter.info { "Generating Compiler Reference Index..." }

        val serializer = CriDataSerializerImpl()
        val criDir = File(workingDir, DATA_PATH).apply { mkdirs() }

        val lookupsFile = File(criDir, LOOKUPS_FILENAME)
        val fileIdsToPathsFile = File(criDir, FILE_IDS_TO_PATHS_FILENAME)
        val subtypesFile = File(criDir, SUBTYPES_FILENAME)

        if (compilationMode is CompilationMode.Rebuild) {
            lookupsFile.delete()
            fileIdsToPathsFile.delete()
            subtypesFile.delete()
        }

        val lookupTracker = services[LookupTracker::class.java] as LookupTrackerImpl
        val lookupData = serializer.serializeLookups(lookupTracker.lookups.toHashMap(), sourceFilesPathConverter)
        // TODO KT-82000 Find better approach for generating CRI data with IC instead of appending new data
        // TODO write better tests for this case
        lookupsFile.appendBytes(lookupData.lookups)
        fileIdsToPathsFile.appendBytes(lookupData.fileIdsToPaths)

        val subtypes = subtypeTracker?.subtypeMap ?: emptyMap()
        subtypesFile.appendBytes(serializer.serializeSubtypes(subtypes))

        reporter.info { "Compiler Reference Index data saved to ${lookupsFile.path}, ${fileIdsToPathsFile.path}, ${subtypesFile.path}" }
        reporter.debug { "Lookups stored: ${lookupTracker.lookups.size()}, subtypes stored: ${subtypes.size}" }
    }
}
