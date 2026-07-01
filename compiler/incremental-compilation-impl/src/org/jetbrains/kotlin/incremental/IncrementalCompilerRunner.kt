/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute.*
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.toGeneratedFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.dirtyFiles.DirtyFilesContainer
import org.jetbrains.kotlin.incremental.dirtyFiles.DirtyFilesProvider
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.snapshots.ConfigurationInputsMap
import org.jetbrains.kotlin.incremental.snapshots.librarySetAddedSentinel
import org.jetbrains.kotlin.incremental.snapshots.librarySetRemovedSentinel
import org.jetbrains.kotlin.incremental.storage.BasicFileToPathConverter
import org.jetbrains.kotlin.incremental.storage.FileLocations
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.incremental.util.ExceptionLocation
import org.jetbrains.kotlin.incremental.util.reportException
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.util.toMetadataVersion
import java.io.File
import java.nio.file.Files

abstract class IncrementalCompilerRunner<
        Args : CommonCompilerArguments,
        CacheManager : IncrementalCachesManager<*>,
        >(
    protected val workingDir: File,
    cacheDirName: String,
    protected val reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    protected val buildHistoryFile: File?,

    /**
     * Output directories of the compilation. These include:
     *   1. The classes output directory
     *   2. [workingDir]
     *   3. Any additional output directories (e.g., classpath snapshot directory or Kapt generated-stubs directory)
     *
     * We will clean these directories when compiling non-incrementally.
     *
     * If this property is not set, the directories to clean will include the first 2 directories above.
     */
    private val outputDirs: Collection<File>?,

    /**
     * Kotlin source files extensions. Set can be extended, usually for scripting purposes
     */
    protected val kotlinSourceFilesExtensions: Set<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,

    /**
     * Various options. Boolean flags, both stable and experimental, should be added there.
     * Non-trivial configuration should NOT be added there.
     */
    protected val icFeatures: IncrementalCompilationFeatures,

    private val compilationCanceledStatus: CompilationCanceledStatus? = null,
) {

    protected open val lookupTrackerDelegate: LookupTracker = LookupTracker.DO_NOTHING
    protected val cacheDirectory = File(workingDir, cacheDirName)
    protected val lastBuildInfoFile = File(workingDir, LAST_BUILD_INFO_FILE_NAME)
    private val abiSnapshotFile = File(workingDir, ABI_SNAPSHOT_FILE_NAME)
    internal val dirtyFilesProvider: DirtyFilesProvider = DirtyFilesProvider(workingDir, kotlinSourceFilesExtensions, reporter)

    /**
     * Creates an instance of [IncrementalCompilationContext] that holds common incremental compilation context mostly required for [CacheManager]
     */
    private fun createIncrementalCompilationContext(
        fileLocations: FileLocations?,
        transaction: CompilationTransaction,
        fragmentContext: FragmentContext? = null,
        compilerGeneratedSyntheticSources: MutableSet<File> = mutableSetOf(),
    ) = IncrementalCompilationContext(
        pathConverterForSourceFiles = fileLocations?.getRelocatablePathConverterForSourceFiles(compilerGeneratedSyntheticSources)
            ?: BasicFileToPathConverter,
        pathConverterForOutputFiles = fileLocations?.getRelocatablePathConverterForOutputFiles(compilerGeneratedSyntheticSources)
            ?: BasicFileToPathConverter,
        transaction = transaction,
        reporter = reporter,
        trackChangesInLookupCache = shouldTrackChangesInLookupCache,
        trackLibrarySetChanges = shouldTrackLibrarySetChanges,
        storeFullFqNamesInLookupCache = shouldStoreFullFqNamesInLookupCache,
        icFeatures = icFeatures,
        fragmentContext = fragmentContext,
        compilerGeneratedSyntheticSources = compilerGeneratedSyntheticSources,
    )

    protected abstract val shouldTrackChangesInLookupCache: Boolean

    /**
     * Whether this runner needs the IC layer to maintain its own snapshot of the library set on the classpath.
     * True for runners that use the build-history-files-based IC approach (JS/Wasm); false for runners using
     * the classpath-snapshot-based approach (JVM), which has its own library change detection.
     */
    protected open val shouldTrackLibrarySetChanges: Boolean = false

    protected abstract val shouldStoreFullFqNamesInLookupCache: Boolean

    protected abstract fun createCacheManager(icContext: IncrementalCompilationContext, args: Args): CacheManager
    protected abstract fun destinationDir(args: Args): File

    /**
     * The set of library files (JARs, KLIBs, or class directories) the IC layer snapshots on its own,
     * so that library changes are detected even when the build system passes [ChangedFiles.DeterminableFiles.ToBeComputed].
     *
     * Only called when [shouldTrackLibrarySetChanges] is true; subclasses that flip that flag must override this.
     */
    protected open fun classpathForLibrarySetSnapshot(args: Args): List<File> =
        error("classpathForLibrarySetSnapshot must be overridden when shouldTrackLibrarySetChanges is true")

    /**
     * The multi-project module info corresponding to the libraries returned by [classpathForLibrarySetSnapshot].
     * Used to derive stable, path-independent identities for local-module libraries; external libraries fall back
     * to a content-hash identity.
     */
    protected open val modulesApiHistory: ModulesApiHistory get() = EmptyModulesApiHistory

    fun compile(
        allSourceFiles: List<File>,
        args: Args,
        messageCollector: MessageCollector,
        changedFiles: ChangedFiles,
        fileLocations: FileLocations? = null, // Must be not-null if the build system needs to support build cache relocatability
        configurationInputs: ConfigurationInputs? = null,
    ): ExitCode = reporter.measure(INCREMENTAL_COMPILATION_DAEMON) {
        reporter.debug {
            "Source changes: $changedFiles"
        }
        if (configurationInputs != null) {
            reporter.debug {
                "Configuration inputs: $configurationInputs"
            }
        }
        val hashedConfigurationInputs = configurationInputs?.computeHashedConfigurationInputs()
        val trackChangedFiles = changedFiles is DeterminableFiles.ToBeComputed
        val result = when (val result = tryCompileIncrementally(allSourceFiles, changedFiles, args, fileLocations, messageCollector, hashedConfigurationInputs)) {
            is ICResult.Completed -> {
                reporter.debug { "Incremental compilation completed" }
                result.exitCode
            }
            is ICResult.RequiresRebuild -> {
                reporter.info { "Non-incremental compilation will be performed: ${result.reason.readableString}" }
                reporter.addAttribute(result.reason)

                compileNonIncrementally(
                    result.reason, allSourceFiles, args, fileLocations, trackChangedFiles = trackChangedFiles, messageCollector, hashedConfigurationInputs
                )
            }
            is ICResult.Failed -> {
                messageCollector.reportException(result.cause, ExceptionLocation.INCREMENTAL_COMPILATION)
                reporter.warn {
                    // The indentation after the first line is intentional (so that this message is distinct from next message)
                    """
                    |Incremental compilation was attempted but failed:
                    |    ${result.reason.readableString}: ${result.cause.stackTraceToString().removeSuffixIfPresent("\n")}
                    |    Falling back to non-incremental compilation (reason = ${result.reason})
                    |    To help us fix this issue, please file a bug at https://youtrack.jetbrains.com/issues/KT with the above stack trace.
                    |    (Be sure to search for the above exception in existing issues first to avoid filing duplicated bugs.)
                    """.trimMargin()
                }
                // TODO: Collect the stack trace too
                reporter.addAttribute(result.reason)

                compileNonIncrementally(
                    result.reason, allSourceFiles, args, fileLocations, trackChangedFiles = trackChangedFiles, messageCollector, hashedConfigurationInputs
                )
            }
        }
        collectSizeMetrics()
        return result
    }

    /** The result when attempting to compile incrementally ([tryCompileIncrementally]). */
    private sealed interface ICResult {

        /** Incremental compilation completed with an [ExitCode]. */
        class Completed(val exitCode: ExitCode) : ICResult

        /** Incremental compilation was not possible for some valid reason (e.g., for a clean build). */
        class RequiresRebuild(val reason: BuildAttribute) : ICResult

        /** Incremental compilation failed with an exception. */
        class Failed(val reason: BuildAttribute, val cause: Throwable) : ICResult
    }

    // Be aware that [tryCompileIncrementally] catches a lot of exceptions internally.
    // So this transformer should be used for very specific things, like cache closing, that are
    // related to the transaction as a whole rather than any compilation step.
    private fun incrementalCompilationExceptionTransformer(t: Throwable): ICResult = when (t) {
        is CachesManagerCloseException -> ICResult.Failed(IC_FAILED_TO_CLOSE_CACHES, t)
        else -> throw t
    }

    /**
     * Attempts to compile incrementally and returns either [ICResult.Completed], [ICResult.RequiresRebuild], or [ICResult.Failed].
     *
     * Note that parts of this function may still throw exceptions that are not caught and wrapped by [ICResult.Failed] because they are not
     * meant to be caught.
     */
    private fun tryCompileIncrementally(
        allSourceFiles: List<File>,
        changedFiles: ChangedFiles,
        args: Args,
        fileLocations: FileLocations?,
        messageCollector: MessageCollector,
        hashedConfigurationInputs: HashedConfigurationInputs?,
    ): ICResult {
        if (changedFiles is ChangedFiles.Unknown) {
            return ICResult.RequiresRebuild(UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
        }
        changedFiles as? DeterminableFiles ?: error("Expected $changedFiles to be an instance of DeterminableFiles")

        val fragmentContext = if (!icFeatures.enableUnsafeIncrementalCompilationForMultiplatform) { //see KT-62686
            FragmentContext.fromCompilerArguments(args)
        } else {
            null
        }

        return createTransaction().runWithin(::incrementalCompilationExceptionTransformer) { transaction ->
            val icContext = createIncrementalCompilationContext(
                fileLocations,
                transaction,
                fragmentContext,
            )
            val caches = createCacheManager(icContext, args).also {
                // this way we make the transaction to be responsible for closing the caches manager
                transaction.cachesManager = it
            }

            // Detect compiler argument changes — force full rebuild if args changed since last build
            if (hashedConfigurationInputs != null) {
                when (val state = caches.inputsCache.configurationInputsMap.checkConfigurationState(hashedConfigurationInputs)) {
                    is ConfigurationInputsMap.ConfigurationState.RequiresRebuild -> {
                        return ICResult.RequiresRebuild(state.reason)
                    }
                    ConfigurationInputsMap.ConfigurationState.UpToDate -> {}
                }
            }

            fun compile(): ICResult {
                // Step 1: Get changed files
                val knownChangedFiles: DeterminableFiles.Known = try {
                    getChangedFiles(changedFiles, allSourceFiles, args, caches)
                } catch (e: Throwable) {
                    return ICResult.Failed(IC_FAILED_TO_GET_CHANGED_FILES, e)
                }

                val classpathAbiSnapshot = if (icFeatures.withAbiSnapshot) getClasspathAbiSnapshot(args) else null

                // Step 2: Compute files to recompile
                val compilationMode = try {
                    reporter.measure(IC_CALCULATE_INITIAL_DIRTY_SET) {
                        calculateSourcesToCompile(caches, knownChangedFiles, args, messageCollector, classpathAbiSnapshot ?: emptyMap())
                    }
                } catch (e: Throwable) {
                    return ICResult.Failed(IC_FAILED_TO_COMPUTE_FILES_TO_RECOMPILE, e)
                }

                if (compilationMode is CompilationMode.Rebuild) {
                    return ICResult.RequiresRebuild(compilationMode.reason)
                }

                val abiSnapshotData = if (icFeatures.withAbiSnapshot) {
                    if (!abiSnapshotFile.exists()) {
                        reporter.debug { "Jar snapshot file does not exist: ${abiSnapshotFile.path}" }
                        return ICResult.RequiresRebuild(NO_ABI_SNAPSHOT)
                    }
                    reporter.info { "Incremental compilation with ABI snapshot enabled" }
                    AbiSnapshotData(
                        snapshot = AbiSnapshotImpl.read(abiSnapshotFile),
                        classpathAbiSnapshot = classpathAbiSnapshot!!
                    )
                } else null

                // Step 3: Compile incrementally
                val exitCode = try {
                    compileImpl(
                        icContext,
                        compilationMode as CompilationMode.Incremental,
                        allSourceFiles,
                        args,
                        caches,
                        abiSnapshotData,
                        messageCollector,
                    )
                } catch (e: RequireRebuildForCorrectnessInKMPException) {
                    return ICResult.RequiresRebuild(UNSAFE_INCREMENTAL_CHANGE_KT_62686)
                } catch (e: Throwable) {
                    return ICResult.Failed(IC_FAILED_TO_COMPILE_INCREMENTALLY, e)
                }

                return ICResult.Completed(exitCode)
            }

            compile().also { icResult ->
                if (icResult is ICResult.Completed && icResult.exitCode == ExitCode.OK) {
                    if (hashedConfigurationInputs != null) {
                        caches.inputsCache.configurationInputsMap.updateHash(hashedConfigurationInputs)
                    }
                    transaction.markAsSuccessful()
                }
            }
        }
    }

    private fun compileNonIncrementally(
        rebuildReason: BuildAttribute,
        allSourceFiles: List<File>,
        args: Args,
        fileLocations: FileLocations?,
        trackChangedFiles: Boolean, // Whether we need to track changes to the source files or the build system already handles it
        messageCollector: MessageCollector,
        hashedConfigurationInputs: HashedConfigurationInputs?
    ): ExitCode {
        reporter.measure(CLEAR_OUTPUT_ON_REBUILD) {
            val mainOutputDirs = setOf(destinationDir(args), workingDir)
            val outputDirsToClean = outputDirs?.also {
                check(it.containsAll(mainOutputDirs)) { "outputDirs is missing classesDir and workingDir: $it" }
            } ?: mainOutputDirs

            reporter.debug { "Cleaning ${outputDirsToClean.size} output directories" }
            cleanOrCreateDirectories(outputDirsToClean)
        }
        val icContext = createIncrementalCompilationContext(
            fileLocations,
            NonRecoverableCompilationTransaction(),
        )
        return createCacheManager(icContext, args).use { caches ->
            if (trackChangedFiles) {
                caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
            }
            // Refresh the library-set snapshot on rebuild too, so that the next incremental build sees
            // an accurate baseline and doesn't treat all classpath entries as "new" again.
            caches.inputsCache.librarySetSnapshotMap?.compareAndUpdate(
                classpathForLibrarySetSnapshot(args),
                modulesApiHistory.modulesInfo,
            )
            val abiSnapshotData = if (icFeatures.withAbiSnapshot) {
                AbiSnapshotData(snapshot = AbiSnapshotImpl(mutableMapOf()), classpathAbiSnapshot = getClasspathAbiSnapshot(args))
            } else null

            val exitCode = compileImpl(icContext, CompilationMode.Rebuild(rebuildReason), allSourceFiles, args, caches, abiSnapshotData, messageCollector)
            if (exitCode == ExitCode.OK) {
                if (hashedConfigurationInputs != null) {
                    caches.inputsCache.configurationInputsMap.updateHash(hashedConfigurationInputs)
                }
            }
            exitCode
        }
    }

    private class AbiSnapshotData(val snapshot: AbiSnapshot, val classpathAbiSnapshot: Map<String, AbiSnapshot>)

    private fun getClasspathAbiSnapshot(args: Args): Map<String, AbiSnapshot> {
        return reporter.measure(SET_UP_ABI_SNAPSHOTS) {
            setupJarDependencies(args, reporter)
        }
    }

    /**
     * Deletes the contents of the given directories (not the directories themselves).
     *
     * If the directories do not yet exist, they will be created.
     */
    private fun cleanOrCreateDirectories(outputDirs: Collection<File>) {
        outputDirs.toSet().forEach {
            when {
                it.isDirectory -> it.deleteDirectoryContents()
                it.isFile -> "Expected a directory but found a regular file: ${it.path}"
                else -> it.createDirectory()
            }
        }
    }

    private fun getChangedFiles(
        changedFiles: DeterminableFiles,
        allSourceFiles: List<File>,
        args: Args,
        caches: CacheManager,
    ): DeterminableFiles.Known {
        val sourceLevelChanges: DeterminableFiles.Known = when (changedFiles) {
            is DeterminableFiles.ToBeComputed -> caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
            is DeterminableFiles.Known -> {
                if (changedFiles.forDependencies) {
                    val moreChangedFiles = caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
                    DeterminableFiles.Known(
                        modified = changedFiles.modified + moreChangedFiles.modified,
                        removed = changedFiles.removed + moreChangedFiles.removed
                    )
                } else {
                    changedFiles
                }
            }
        }

        // Library set snapshotting runs in both branches for runners that opted in via shouldTrackLibrarySetChanges.
        // SourcesChanges only models source changes; library changes flow through this separate, path-independent
        // snapshot map so that dirtyFiles/getClasspathChanges can pick up rebuilt local modules (via history files)
        // and any change to an external library (which has no history file → full rebuild).
        val librarySetSnapshotMap = caches.inputsCache.librarySetSnapshotMap ?: return sourceLevelChanges
        val librarySetChanges = librarySetSnapshotMap.compareAndUpdate(
            classpathForLibrarySetSnapshot(args),
            modulesApiHistory.modulesInfo,
        )
        if (librarySetChanges.modifiedFiles.isEmpty() &&
            librarySetChanges.addedKeys.isEmpty() &&
            librarySetChanges.removedKeys.isEmpty()
        ) {
            return sourceLevelChanges
        }
        // Removed and added library identities can't be expressed as real Files (we store no path for them
        // in the removed case, and we deliberately do not want the added case to flow through the normal
        // modified-jar pipeline since the library's own history would only contain a partial delta).
        // We carry both as sentinel Files; getClasspathChanges recognizes the sentinel prefixes and treats
        // them as "the classpath set changed" → ChangesEither.Unknown → full rebuild, which is the only
        // sound answer for any library add/remove in the history-files-based IC pipeline.
        // Anyway, the history files-based approach does not have any solution for changes in dependencies
        // with no history files other than rebuild.
        val removedLibrarySentinels = librarySetChanges.removedKeys.map { librarySetRemovedSentinel(it) }
        val addedLibrarySentinels = librarySetChanges.addedKeys.map { librarySetAddedSentinel(it) }
        return DeterminableFiles.Known(
            modified = sourceLevelChanges.modified + librarySetChanges.modifiedFiles + addedLibrarySentinels,
            removed = sourceLevelChanges.removed + removedLibrarySentinels,
        )
    }

    protected abstract fun calculateSourcesToCompile(
        caches: CacheManager,
        changedFiles: DeterminableFiles.Known,
        args: Args,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot>,
    ): CompilationMode

    protected open fun setupJarDependencies(
        args: Args,
        reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    ): Map<String, AbiSnapshot> = emptyMap()

    sealed class CompilationMode {
        class Incremental(val dirtyFiles: DirtyFilesContainer) : CompilationMode()
        class Rebuild(val reason: BuildAttribute) : CompilationMode()
    }

    protected abstract fun updateCaches(
        services: Services,
        caches: CacheManager,
        generatedFiles: List<GeneratedFile>,
        changesCollector: ChangesCollector,
    )

    protected open fun additionalDirtyFiles(caches: CacheManager, generatedFiles: List<GeneratedFile>, services: Services): Iterable<File> =
        emptyList()

    protected open fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
        emptyList()

    protected open fun makeServices(
        args: Args,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        fileMappingTracker: ICFileMappingTracker,
        caches: CacheManager,
        dirtySources: Set<File>,
        isIncremental: Boolean,
        compilationCanceledStatus: CompilationCanceledStatus,
    ): Services.Builder =
        Services.Builder().apply {
            register(LookupTracker::class.java, lookupTracker)
            register(ExpectActualTracker::class.java, expectActualTracker)
            register(CompilationCanceledStatus::class.java, compilationCanceledStatus)
            register(ICFileMappingTracker::class.java, fileMappingTracker)
        }

    protected abstract fun runCompiler(
        sourcesToCompile: List<File>,
        args: Args,
        caches: CacheManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean,
    ): Pair<ExitCode, Collection<File>>

    private fun compileImpl(
        icContext: IncrementalCompilationContext,
        compilationMode: CompilationMode,
        allSourceFiles: List<File>,
        args: Args,
        caches: CacheManager,
        abiSnapshotData: AbiSnapshotData?, // Not null iff withAbiSnapshot = true
        messageCollector: MessageCollector,
    ): ExitCode {
        performWorkBeforeCompilation(compilationMode, args)

        val allKotlinFiles = allSourceFiles.filter { it.isKotlinFile(kotlinSourceFilesExtensions) }
        val exitCode = doCompile(icContext, caches, compilationMode, allKotlinFiles, args, abiSnapshotData, messageCollector)

        performWorkAfterCompilation(compilationMode, exitCode, caches)
        return exitCode
    }

    private fun createTransaction() = if (icFeatures.preciseCompilationResultsBackup) {
        RecoverableCompilationTransaction(reporter, Files.createTempDirectory("kotlin-backups"))
    } else {
        NonRecoverableCompilationTransaction()
    }

    protected open fun performWorkBeforeCompilation(compilationMode: CompilationMode, args: Args) {}

    protected open fun performWorkAfterCompilation(compilationMode: CompilationMode, exitCode: ExitCode, caches: CacheManager) {}

    private fun collectSizeMetrics() {
        reporter.measure(CALCULATE_OUTPUT_SIZE) {
            reporter.addMetric(
                SNAPSHOT_SIZE,
                (buildHistoryFile?.length() ?: 0) + lastBuildInfoFile.length() + abiSnapshotFile.length()
            )
            reporter.addMetric(
                CACHE_DIRECTORY_SIZE,
                cacheDirectory.walk().filter { it.isFile }.sumOf { it.length() })
        }
    }

    private fun doCompile(
        icContext: IncrementalCompilationContext,
        caches: CacheManager,
        compilationMode: CompilationMode,
        allKotlinSources: List<File>,
        args: Args,
        abiSnapshotData: AbiSnapshotData?, // Not null iff withAbiSnapshot = true
        originalMessageCollector: MessageCollector,
    ): ExitCode {
        val dirtySources = when (compilationMode) {
            is CompilationMode.Incremental -> compilationMode.dirtyFiles.toMutableLinkedSet()
            is CompilationMode.Rebuild -> LinkedHashSet(allKotlinSources)
        }

        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis(), abiSnapshotData?.classpathAbiSnapshot ?: emptyMap())
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()
        val allDirtySources = HashSet<File>()
        val transaction = icContext.transaction

        var exitCode = ExitCode.OK

        // TODO: ideally we should read arguments not here but at earlier stages
        val metadataVersionFromLanguageVersion =
            LanguageVersion.fromVersionString(args.languageVersion)?.toMetadataVersion() ?: MetadataVersion.INSTANCE

        while (dirtySources.any() || runWithNoDirtyKotlinSources(caches)) {
            val complementaryFiles = caches.platformCache.getComplementaryFilesRecursive(dirtySources)
            dirtySources.addAll(complementaryFiles)
            dirtySources.addAll(caches.compilerPluginFilesCache.getSourceFilesReferencedByPlugins())
            dirtySources.addAll(caches.compilerPluginFilesCache.getSourceFilesGeneratedByPlugins())
            caches.platformCache.markDirty(dirtySources)
            caches.inputsCache.removeOutputForSourceFiles(dirtySources)
            caches.compilerPluginFilesCache.removeOutputsGeneratedByPlugins()

            val lookupTracker = LookupTrackerImpl(lookupTrackerDelegate)
            val expectActualTracker = ExpectActualTrackerImpl()

            val outputItemsCollector = OutputItemsCollectorImpl()
            val transactionOutputsRegistrar = TransactionOutputsRegistrar(transaction, outputItemsCollector)
            val fileMappingTracker = ICFileMappingTrackerImpl(transactionOutputsRegistrar)

            val [sourcesToCompile, removedKotlinSources] = dirtySources.partition { it.exists() && allKotlinSources.contains(it) }

            icContext.fragmentContext?.let {
                if (it.dirtySetTouchesNonLeafFragments(sourcesToCompile)) {
                    throw RequireRebuildForCorrectnessInKMPException()
                }
            }

            val services = makeServices(
                args, lookupTracker, expectActualTracker, fileMappingTracker, caches,
                dirtySources.toSet(),
                compilationMode is CompilationMode.Incremental,
                compilationCanceledStatus ?: EmptyCompilationCanceledStatus
            ).build()

            args.reportOutputFiles = true
            val bufferingMessageCollector = MessageCollectorImpl()

            val compiledSources = reporter.measure(COMPILATION_ROUND) {
                runCompiler(
                    sourcesToCompile, args, caches, services, bufferingMessageCollector,
                    allKotlinSources, compilationMode is CompilationMode.Incremental
                )
            }.let { [ec, compiled] ->
                exitCode = ec
                compiled
            }
            icContext.compilerGeneratedSyntheticSources.clear()
            icContext.compilerGeneratedSyntheticSources.addAll(outputItemsCollector.sourceFileGeneratedForPlugin)

            dirtySources.addAll(compiledSources)
            allDirtySources.addAll(dirtySources)
            dirtyFilesProvider.cachedHistory.store(transaction, allDirtySources)

            outputItemsCollector.outputs.firstOrNull { !it.outputFile.exists() }?.let { brokenOutput ->
                reporter.warn { "Expected output item, but file doesn't exist: ${brokenOutput.outputFile}" }
                reporter.reportCompileIteration(compilationMode is CompilationMode.Incremental, compiledSources, exitCode)
                bufferingMessageCollector.forward(originalMessageCollector)
                check(exitCode != ExitCode.OK) { "Expected compiler error, but got exitCode=$exitCode" }
                break
            }

            val generatedFiles = outputItemsCollector.outputs.map {
                it.toGeneratedFile(metadataVersionFromLanguageVersion)
            }
            if (compilationMode is CompilationMode.Incremental) {
                // todo: feels dirty, can this be refactored?
                val dirtySourcesSet = dirtySources.toHashSet()
                val additionalDirtyFiles = additionalDirtyFiles(caches, generatedFiles, services).filter { it !in dirtySourcesSet }
                if (additionalDirtyFiles.isNotEmpty()) {
                    dirtySources.addAll(additionalDirtyFiles)
                    generatedFiles.forEach { transaction.deleteFile(it.outputFile.toPath()) }
                    continue
                }
            }

            reporter.reportCompileIteration(compilationMode is CompilationMode.Incremental, compiledSources, exitCode)
            bufferingMessageCollector.forward(originalMessageCollector)

            if (exitCode != ExitCode.OK) break

            dirtyFilesProvider.cachedHistory.clear(withTransaction = transaction)

            val changesCollector = ChangesCollector()
            reporter.measure(IC_UPDATE_CACHES) {
                caches.platformCache.updateComplementaryFiles(dirtySources, expectActualTracker)
                caches.inputsCache.registerOutputForSourceFiles(generatedFiles)
                caches.lookupCache.update(lookupTracker, sourcesToCompile, removedKotlinSources)
                caches.recordCompilerPluginFilesCaches(outputItemsCollector)
                updateCaches(services, caches, generatedFiles, changesCollector)
            }

            reporter.measure(IC_GEN_COMPILER_REF_INDEX) {
                generateCompilerRefIndexIfNeeded(services, icContext.pathConverterForSourceFiles, compilationMode)
            }

            if (compilationMode is CompilationMode.Rebuild) {
                if (icFeatures.withAbiSnapshot) {
                    abiSnapshotData!!.snapshot.protos.putAll(changesCollector.protoDataChanges())
                }
                break
            }

            (
                val dirtyLookupSymbols, val dirtyClassFqNames = dirtyClassesFqNames, val forceRecompile = dirtyClassesFqNamesForceRecompile
            ) =
                changesCollector.getChangedAndImpactedSymbols(
                    listOf(caches.platformCache),
                    reporter
                )
            val compiledInThisIterationSet = sourcesToCompile.toHashSet()

            val forceToRecompileFiles = mapClassesFqNamesToFiles(listOf(caches.platformCache), forceRecompile, reporter)
            with(dirtySources) {
                clear()
                addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = compiledInThisIterationSet))
                addAll(
                    mapClassesFqNamesToFiles(
                        listOf(caches.platformCache),
                        dirtyClassFqNames,
                        reporter,
                        excludes = compiledInThisIterationSet
                    )
                )
                if (!compiledInThisIterationSet.containsAll(forceToRecompileFiles)) {
                    addAll(forceToRecompileFiles)
                }
                if (icFeatures.enableMonotonousIncrementalCompileSetExpansion) {
                    if (dirtySources.isNotEmpty()) {
                        // At this point we have determined that some new source files need to be recompiled,
                        // and we can add previously compiled files to the dirtySources set for logical consistency.
                        // (Take note that outer loop triggers compilation steps while there are not-yet-recompiled affected files.)
                        addAll(compiledInThisIterationSet)
                    }
                }
            }

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)

            //update
            if (icFeatures.withAbiSnapshot) {
                //TODO(valtman) check method/ kts class remove
                changesCollector.protoDataRemoved().forEach { abiSnapshotData!!.snapshot.protos.remove(it) }
                abiSnapshotData!!.snapshot.protos.putAll(changesCollector.protoDataChanges())
            }
        }

        if (exitCode == ExitCode.OK) {
            reporter.measure(STORE_BUILD_INFO) {
                BuildInfo.write(icContext, currentBuildInfo, lastBuildInfoFile)

                //write abi snapshot
                if (icFeatures.withAbiSnapshot) {
                    //TODO(valtman) check method/class remove
                    AbiSnapshotImpl.write(icContext, abiSnapshotData!!.snapshot, abiSnapshotFile)
                }
            }
        }
        if (exitCode == ExitCode.OK && compilationMode is CompilationMode.Incremental) {
            buildDirtyLookupSymbols.addAll(additionalDirtyLookupSymbols())
        }

        val dirtyData = DirtyData(buildDirtyLookupSymbols, buildDirtyFqNames)
        processChangesAfterBuild(icContext, compilationMode, currentBuildInfo, dirtyData)

        return exitCode
    }

    protected open fun generateCompilerRefIndexIfNeeded(
        services: Services,
        sourceFilesPathConverter: FileToPathConverter,
        compilationMode: CompilationMode,
    ): Unit = Unit

    open fun runWithNoDirtyKotlinSources(caches: CacheManager): Boolean = false

    private fun processChangesAfterBuild(
        icContext: IncrementalCompilationContext,
        compilationMode: CompilationMode,
        currentBuildInfo: BuildInfo,
        dirtyData: DirtyData,
    ) {
        if (buildHistoryFile == null) return
        reporter.measure(IC_WRITE_HISTORY_FILE) {
            val prevDiffs = BuildDiffsStorage.readFromFile(buildHistoryFile, reporter)?.buildDiffs ?: emptyList()
            val newDiff = if (compilationMode is CompilationMode.Incremental) {
                BuildDifference(currentBuildInfo.startTS, true, dirtyData)
            } else {
                val emptyDirtyData = DirtyData()
                BuildDifference(currentBuildInfo.startTS, false, emptyDirtyData)
            }

            BuildDiffsStorage.writeToFile(icContext, buildHistoryFile, BuildDiffsStorage(prevDiffs + newDiff))
        }
    }

    companion object {
        const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
        const val LAST_BUILD_INFO_FILE_NAME = "last-build.bin"
        const val ABI_SNAPSHOT_FILE_NAME = "abi-snapshot.bin"
        const val BUILD_HISTORY_FILE_NAME = "build-history.bin"
    }

    private object EmptyCompilationCanceledStatus : CompilationCanceledStatus {
        override fun checkCanceled() {
        }
    }
}

@Deprecated("Temporary function to reuse the logic. KT-62759")
fun extractKotlinSourcesFromFreeCompilerArguments(
    compilerArguments: CommonCompilerArguments,
    kotlinFilenameExtensions: Set<String>,
    includeJavaSources: Boolean, // Java files should be passed too to the incremental JVM compiler so that it could track changes in Java sources
): List<File> {
    val kotlinDotExtensions = kotlinFilenameExtensions.map { ".$it" }
    val freeArgs = arrayListOf<String>()
    val allKotlinFiles = arrayListOf<File>()
    for (arg in compilerArguments.freeArgs) {
        val file = File(arg)
        if (!file.isFile) {
            freeArgs.add(arg)
            continue
        }

        if (kotlinDotExtensions.any { ext -> file.path.endsWith(ext, ignoreCase = true) }) {
            // a kotlin source file
            allKotlinFiles.add(file)
        } else if (includeJavaSources && file.path.endsWith(".java", ignoreCase = true)) {
            // a java source file
            allKotlinFiles.add(file)
            freeArgs.add(arg)
        } else {
            // an unknown file
            freeArgs.add(arg)
        }
    }
    compilerArguments.freeArgs = freeArgs
    return allKotlinFiles
}
