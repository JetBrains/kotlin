/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.report.*
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.dirtyFiles.getClasspathChanges
import org.jetbrains.kotlin.incremental.dirtyFiles.getRemovedClassesChanges
import org.jetbrains.kotlin.incremental.js.*
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File

abstract class IncrementalKlibCompilerRunner<Args : CommonKlibBasedCompilerArguments>(
    workingDir: File,
    cachesDirName: String,
    reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    buildHistoryFile: File?,
    override val modulesApiHistory: ModulesApiHistory,
    icFeatures: IncrementalCompilationFeatures,
    pluginsLoader: PluginsLoader?,
    override val lookupTrackerDelegate: LookupTracker,
) : IncrementalCompilerRunner<Args, IncrementalKlibCachesManager>(
    workingDir,
    cachesDirName,
    reporter,
    buildHistoryFile = buildHistoryFile,
    outputDirs = null,
    icFeatures = icFeatures,
    pluginsLoader = pluginsLoader,
) {
    override val shouldTrackChangesInLookupCache
        get() = false

    override val shouldTrackLibrarySetChanges
        get() = true

    override val shouldStoreFullFqNamesInLookupCache
        get() = false

    override fun calculateSourcesToCompile(
        caches: IncrementalKlibCachesManager,
        changedFiles: ChangedFiles.DeterminableFiles.Known,
        args: Args,
        messageCollector: MessageCollector,
    ): CompilationMode {
        if (buildHistoryFile == null) {
            error("The build is configured to use the build-history based IC approach, but doesn't specify the buildHistoryFile")
        }
        if (!buildHistoryFile.isFile) {
            return CompilationMode.Rebuild(BuildAttribute.NO_BUILD_HISTORY)
        }
        val lastBuildInfo =
            BuildInfo.read(lastBuildInfoFile, messageCollector) ?: return CompilationMode.Rebuild(BuildAttribute.INVALID_LAST_BUILD_INFO)

        val dirtyFiles = dirtyFilesProvider.getInitializedDirtyFiles(caches, changedFiles)

        val libs = classpathForLibrarySetSnapshot(args)
        //TODO(valtman) check for JS
        val classpathChanges = getClasspathChanges(libs, changedFiles, lastBuildInfo, modulesApiHistory, reporter)

        when (classpathChanges) {
            is ChangesEither.Unknown -> {
                reporter.info { "Could not get classpath's changes: ${classpathChanges.reason}" }
                return CompilationMode.Rebuild(classpathChanges.reason)
            }
            is ChangesEither.Known -> {
                dirtyFiles.addByDirtySymbols(classpathChanges.lookupSymbols)
                dirtyFiles.addByDirtyClasses(classpathChanges.fqNames)
            }
        }

        val removedClassesChanges = getRemovedClassesChanges(caches, changedFiles, kotlinSourceFilesExtensions, reporter)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNamesForceRecompile)

        if (dirtyFiles.isEmpty() && changedFiles.removed.isNotEmpty()) {
            return CompilationMode.Rebuild(BuildAttribute.DEP_CHANGE_REMOVED_ENTRY)
        }
        return CompilationMode.Incremental(dirtyFiles)
    }

    override fun makeServices(
        args: Args,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        fileMappingTracker: ICFileMappingTracker,
        caches: IncrementalKlibCachesManager,
        dirtySources: Set<File>,
        isIncremental: Boolean,
        compilationCanceledStatus: CompilationCanceledStatus,
    ): Services.Builder =
        super.makeServices(
            args,
            lookupTracker,
            expectActualTracker,
            fileMappingTracker,
            caches,
            dirtySources,
            isIncremental,
            compilationCanceledStatus
        ).apply {
            if (isIncremental) {
                val nextRoundChecker = IncrementalNextRoundCheckerImpl(caches, dirtySources, reporter)
                register(IncrementalNextRoundChecker::class.java, nextRoundChecker)
                register(IncrementalDataProvider::class.java, getIncrementalDataProvider(args, caches))
            }

            register(
                IncrementalResultsConsumer::class.java,
                getIncrementalResultsConsumer(args)
            )
        }

    private fun getIncrementalResultsConsumer(args: CommonKlibBasedCompilerArguments) =
        if (args.multiPlatform) KmpIncrementalResultsConsumer() else IncrementalResultsConsumerImpl()

    private fun getIncrementalDataProvider(args: CommonKlibBasedCompilerArguments, caches: IncrementalKlibCachesManager) =
        if (args.multiPlatform) KmpIncrementalDataProviderFromCache(caches.platformCache) else IncrementalDataProviderFromCache(caches.platformCache)

    override fun updateCaches(
        services: Services,
        caches: IncrementalKlibCachesManager,
        generatedFiles: List<GeneratedFile>,
        changesCollector: ChangesCollector,
    ) {
        val incrementalResults = services[IncrementalResultsConsumer::class.java] as IncrementalResultsConsumerImpl

        val klibCache = caches.platformCache

        klibCache.compareAndUpdate(incrementalResults, changesCollector)
        klibCache.clearCacheForRemovedClasses(changesCollector)
    }

    protected abstract fun getCompiler(args: Args): CLICompiler<Args>

    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: Args,
        caches: IncrementalKlibCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean,
    ): Pair<ExitCode, Collection<File>> {
        val freeArgsBackup = args.freeArgs
        var compiler: CLICompiler<Args>? = null
        try {
            args.freeArgs += sourcesToCompile.map { it.absolutePath }
            compiler = getCompiler(args)
            return (compiler.exec(messageCollector, services, arguments = args) to sourcesToCompile)
        } finally {
            args.freeArgs = freeArgsBackup
            compiler?.let { reporter.reportPerformanceData(it.defaultPerformanceManager.unitStats) }
        }
    }

    override fun additionalDirtyFiles(
        caches: IncrementalKlibCachesManager,
        generatedFiles: List<GeneratedFile>,
        services: Services,
        dirtySources: Set<File>,
    ): List<File> {
        val additionalDirtyFiles: Set<File> =
            (services[IncrementalNextRoundChecker::class.java] as IncrementalNextRoundCheckerImpl).newDirtySources

        return additionalDirtyFiles.filter { it !in dirtySources }
    }

    class IncrementalNextRoundCheckerImpl(
        private val caches: IncrementalKlibCachesManager,
        private val sourcesToCompile: Set<File>,
        private val reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    ) : IncrementalNextRoundChecker {
        val newDirtySources = HashSet<File>()

        private val translatedFiles = HashMap<File, TranslationResultValue>()

        override fun checkProtoChanges(sourceFile: File, packagePartMetadata: ByteArray) {
            translatedFiles[sourceFile] = TranslationResultValue(packagePartMetadata)
        }

        override fun shouldGoToNextRound(): Boolean {
            val changesCollector = ChangesCollector()
            // todo: split compare and update (or cache comparing)
            caches.platformCache.compare(translatedFiles, changesCollector)
            (val dirtyLookupSymbols, val dirtyClassFqNames = dirtyClassesFqNames) = changesCollector.getChangedAndImpactedSymbols(
                listOf(
                    caches.platformCache
                ), reporter
            )
            // todo unify with main cycle
            newDirtySources.addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = sourcesToCompile))
            newDirtySources.addAll(
                mapClassesFqNamesToFiles(
                    listOf(caches.platformCache),
                    dirtyClassFqNames,
                    reporter,
                    excludes = sourcesToCompile
                )
            )
            return newDirtySources.isNotEmpty()
        }
    }
}
