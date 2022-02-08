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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.MessageCollectorToOutputItemsCollectorAdapter
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.SimpleOutputItem
import org.jetbrains.kotlin.compilerRunner.toGeneratedFile
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.parsing.classesFqNames
import org.jetbrains.kotlin.incremental.util.BufferingMessageCollector
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File

abstract class IncrementalCompilerRunner<
        Args : CommonCompilerArguments,
        CacheManager : IncrementalCachesManager<*>
        >(
    private val workingDir: File,
    cacheDirName: String,
    protected val reporter: BuildReporter,
    private val buildHistoryFile: File,
    // there might be some additional output directories (e.g. for generated java in kapt)
    // to remove them correctly on rebuild, we pass them as additional argument
    private val additionalOutputFiles: Collection<File> = emptyList()
) {

    protected val cacheDirectory = File(workingDir, cacheDirName)
    private val dirtySourcesSinceLastTimeFile = File(workingDir, DIRTY_SOURCES_FILE_NAME)
    protected val lastBuildInfoFile = File(workingDir, LAST_BUILD_INFO_FILE_NAME)
    protected val abiSnapshotFile = File(workingDir, ABI_SNAPSHOT_FILE_NAME)
    protected open val kotlinSourceFilesExtensions: List<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS

    //TODO(valtman) temporal measure to ensure quick disable, should be deleted after successful release
    protected val withSnapshot: Boolean = CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_CLASSPATH_SNAPSHOTS.value.toBooleanLenient() ?: false

    protected abstract fun createCacheManager(args: Args, projectDir: File?): CacheManager
    protected abstract fun destinationDir(args: Args): File

    fun compile(
        allSourceFiles: List<File>,
        args: Args,
        messageCollector: MessageCollector,
        // when [providedChangedFiles] is not null, changes are provided by external system (e.g. Gradle)
        // otherwise we track source files changes ourselves.
        providedChangedFiles: ChangedFiles?,
        projectDir: File? = null
    ): ExitCode = reporter.measure(BuildTime.INCREMENTAL_COMPILATION) {
        compileImpl(allSourceFiles, args, messageCollector, providedChangedFiles, projectDir)
    }

    private fun compileImpl(
        allSourceFiles: List<File>,
        args: Args,
        messageCollector: MessageCollector,
        providedChangedFiles: ChangedFiles?,
        projectDir: File? = null
    ): ExitCode {
        var caches = createCacheManager(args, projectDir)

        if (withSnapshot) {
            reporter.report { "Incremental compilation with ABI snapshot enabled" }
        }
        //TODO if abi-snapshot is corrupted unable to rebuild. Should roll back to withSnapshot = false?
        val classpathAbiSnapshot =
            if (withSnapshot) {
                reporter.measure(BuildTime.SET_UP_ABI_SNAPSHOTS) {
                    setupJarDependencies(args, withSnapshot, reporter)
                }
            } else {
                emptyMap()
            }

        fun rebuild(reason: BuildAttribute): ExitCode {
            reporter.report { "Non-incremental compilation will be performed: $reason" }
            caches.close(false)
            // todo: we can recompile all files incrementally (not cleaning caches), so rebuild won't propagate
            reporter.measure(BuildTime.CLEAR_OUTPUT_ON_REBUILD) {
                cleanOutputsAndLocalStateOnRebuild(args)
            }
            caches = createCacheManager(args, projectDir)
            if (providedChangedFiles == null) {
                caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
            }
            val allKotlinFiles = allSourceFiles.filter { it.isKotlinFile(kotlinSourceFilesExtensions) }
            return compileIncrementally(args, caches, allKotlinFiles, CompilationMode.Rebuild(reason), messageCollector, withSnapshot,
                                        classpathAbiSnapshot = classpathAbiSnapshot)
        }

        // If compilation has crashed or we failed to close caches we have to clear them
        var cachesMayBeCorrupted = true
        return try {
            val changedFiles = when (providedChangedFiles) {
                is ChangedFiles.Dependencies -> {
                    val changedSources = caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
                    ChangedFiles.Known(
                        providedChangedFiles.modified + changedSources.modified,
                        providedChangedFiles.removed + changedSources.removed
                    )
                }
                null -> caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
                else -> providedChangedFiles
            }

            // Check whether the cache directory is populated (note that it may be deleted upon a Gradle build cache hit if the directory is
            // marked as @LocalState in the Gradle task).
            val cacheDirectoryNotPopulated = cacheDirectory.walk().none { it.isFile }

            val compilationMode = if (cacheDirectoryNotPopulated) {
                CompilationMode.Rebuild(BuildAttribute.CACHE_DIRECTORY_NOT_POPULATED)
            } else {
                sourcesToCompile(caches, changedFiles, args, messageCollector, classpathAbiSnapshot)
            }

            val exitCode = when (compilationMode) {
                is CompilationMode.Incremental -> {
                    if (withSnapshot) {
                        val abiSnapshot = AbiSnapshotImpl.read(abiSnapshotFile, reporter)
                        if (abiSnapshot != null) {
                            compileIncrementally(
                                args,
                                caches,
                                allSourceFiles,
                                compilationMode,
                                messageCollector,
                                withSnapshot,
                                abiSnapshot,
                                classpathAbiSnapshot
                            )
                        } else {
                            rebuild(BuildAttribute.NO_ABI_SNAPSHOT)
                        }
                    } else {
                        compileIncrementally(
                            args,
                            caches,
                            allSourceFiles,
                            compilationMode,
                            messageCollector,
                            withSnapshot
                        )
                    }
                }
                is CompilationMode.Rebuild -> {
                    rebuild(compilationMode.reason)
                }
            }

            if (exitCode == ExitCode.OK) {
                performWorkAfterSuccessfulCompilation(caches)
            }

            if (!caches.close(flush = true)) throw RuntimeException("Could not flush caches")
            // Here we should analyze exit code of compiler. E.g. compiler failure should lead to caches rebuild,
            // but now JsKlib compiler reports invalid exit code.
            cachesMayBeCorrupted = false

            reporter.measure(BuildTime.CALCULATE_OUTPUT_SIZE) {
                reporter.addMetric(
                    BuildPerformanceMetric.SNAPSHOT_SIZE,
                    buildHistoryFile.length() + lastBuildInfoFile.length() + abiSnapshotFile.length()
                )
                if (cacheDirectory.exists() && cacheDirectory.isDirectory()) {
                    cacheDirectory.walkTopDown().filter { it.isFile }.map { it.length() }.sum().let {
                        reporter.addMetric(BuildPerformanceMetric.CACHE_DIRECTORY_SIZE, it)
                    }
                }
            }
            return exitCode
        } catch (e: Exception) { // todo: catch only cache corruption
            // todo: warn?
            reporter.report { "Rebuilding because of possible caches corruption: $e" }
            rebuild(BuildAttribute.CACHE_CORRUPTION)
        } finally {
            if (cachesMayBeCorrupted) {
                cleanOutputsAndLocalStateOnRebuild(args)
            }
        }
    }

    /**
     * Deletes output files and contents of output directories on rebuild, including `@LocalState` files/directories.
     *
     * If the directories do not yet exist, they will be created.
     */
    private fun cleanOutputsAndLocalStateOnRebuild(args: Args) {
        // Use Set as additionalOutputFiles may already contain destinationDir and workingDir
        val outputFiles = setOf(destinationDir(args), workingDir) + additionalOutputFiles

        reporter.reportVerbose { "Cleaning outputs on rebuild" }
        outputFiles.forEach {
            when {
                it.isDirectory -> {
                    reporter.reportVerbose { "  Deleting contents of directory '${it.path}'" }
                    it.cleanDirectoryContents()
                }
                it.isFile -> {
                    reporter.reportVerbose { "  Deleting file '${it.path}'" }
                    it.forceDeleteRecursively()
                }
            }
        }
    }

    private fun sourcesToCompile(
        caches: CacheManager,
        changedFiles: ChangedFiles,
        args: Args,
        messageCollector: MessageCollector,
        dependenciesAbiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode =
        when (changedFiles) {
            is ChangedFiles.Known -> calculateSourcesToCompile(caches, changedFiles, args, messageCollector, dependenciesAbiSnapshots)
            is ChangedFiles.Unknown -> CompilationMode.Rebuild(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ChangedFiles.Dependencies -> error("Unexpected ChangedFiles type (ChangedFiles.Dependencies)")
        }

    private fun calculateSourcesToCompile(
        caches: CacheManager, changedFiles: ChangedFiles.Known, args: Args, messageCollector: MessageCollector,
        abiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode =
        reporter.measure(BuildTime.IC_CALCULATE_INITIAL_DIRTY_SET) {
            calculateSourcesToCompileImpl(caches, changedFiles, args, messageCollector, abiSnapshots)
        }

    protected abstract fun calculateSourcesToCompileImpl(
        caches: CacheManager,
        changedFiles: ChangedFiles.Known,
        args: Args,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode

    protected open fun setupJarDependencies(args: Args, withSnapshot: Boolean, reporter: BuildReporter): Map<String, AbiSnapshot> = mapOf()

    protected fun initDirtyFiles(dirtyFiles: DirtyFilesContainer, changedFiles: ChangedFiles.Known) {
        dirtyFiles.add(changedFiles.modified, "was modified since last time")
        dirtyFiles.add(changedFiles.removed, "was removed since last time")

        if (dirtySourcesSinceLastTimeFile.exists()) {
            val files = dirtySourcesSinceLastTimeFile.readLines().map(::File)
            dirtyFiles.add(files, "was not compiled last time")
        }
    }

    protected sealed class CompilationMode {
        class Incremental(val dirtyFiles: DirtyFilesContainer) : CompilationMode()
        class Rebuild(val reason: BuildAttribute) : CompilationMode()
    }

    protected abstract fun updateCaches(
        services: Services,
        caches: CacheManager,
        generatedFiles: List<GeneratedFile>,
        changesCollector: ChangesCollector
    )

    protected open fun preBuildHook(args: Args, compilationMode: CompilationMode) {}
    protected open fun additionalDirtyFiles(caches: CacheManager, generatedFiles: List<GeneratedFile>, services: Services): Iterable<File> =
        emptyList()

    protected open fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
        emptyList()

    protected open fun makeServices(
        args: Args,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        caches: CacheManager,
        dirtySources: Set<File>,
        isIncremental: Boolean
    ): Services.Builder =
        Services.Builder().apply {
            register(LookupTracker::class.java, lookupTracker)
            register(ExpectActualTracker::class.java, expectActualTracker)
            register(CompilationCanceledStatus::class.java, EmptyCompilationCanceledStatus)
        }

    protected abstract fun runCompiler(
        sourcesToCompile: Set<File>,
        args: Args,
        caches: CacheManager,
        services: Services,
        messageCollector: MessageCollector
    ): ExitCode

    private fun compileIncrementally(
        args: Args,
        caches: CacheManager,
        allKotlinSources: List<File>,
        compilationMode: CompilationMode,
        originalMessageCollector: MessageCollector,
        withSnapshot: Boolean,
        abiSnapshot: AbiSnapshot = AbiSnapshotImpl(mutableMapOf()),
        classpathAbiSnapshot: Map<String, AbiSnapshot> = HashMap()
    ): ExitCode {
        preBuildHook(args, compilationMode)

        val buildTimeMode: BuildTime
        val dirtySources = when (compilationMode) {
            is CompilationMode.Incremental -> {
                buildTimeMode = BuildTime.INCREMENTAL_ITERATION
                compilationMode.dirtyFiles.toMutableList()
            }
            is CompilationMode.Rebuild -> {
                buildTimeMode = BuildTime.NON_INCREMENTAL_ITERATION
                reporter.addAttribute(compilationMode.reason)
                allKotlinSources.toMutableList()
            }
        }

        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis(), classpathAbiSnapshot)
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()
        val allDirtySources = HashSet<File>()

        var exitCode = ExitCode.OK

        while (dirtySources.any() || runWithNoDirtyKotlinSources(caches)) {
            val complementaryFiles = caches.platformCache.getComplementaryFilesRecursive(dirtySources)
            dirtySources.addAll(complementaryFiles)
            caches.platformCache.markDirty(dirtySources)
            caches.inputsCache.removeOutputForSourceFiles(dirtySources)

            val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
            val expectActualTracker = ExpectActualTrackerImpl()
            //TODO(valtman) sourceToCompile calculate based on abiSnapshot
            val (sourcesToCompile, removedKotlinSources) = dirtySources.partition(File::exists)

            allDirtySources.addAll(dirtySources)
            val text = allDirtySources.joinToString(separator = System.getProperty("line.separator")) { it.canonicalPath }
            dirtySourcesSinceLastTimeFile.writeText(text)

            val services = makeServices(
                args, lookupTracker, expectActualTracker, caches,
                dirtySources.toSet(), compilationMode is CompilationMode.Incremental
            ).build()

            args.reportOutputFiles = true
            val outputItemsCollector = OutputItemsCollectorImpl()
            val bufferingMessageCollector = BufferingMessageCollector()
            val messageCollectorAdapter = MessageCollectorToOutputItemsCollectorAdapter(bufferingMessageCollector, outputItemsCollector)

            exitCode = reporter.measure(buildTimeMode) {
                runCompiler(sourcesToCompile.toSet(), args, caches, services, messageCollectorAdapter)
            }

            val generatedFiles = outputItemsCollector.outputs.map(SimpleOutputItem::toGeneratedFile)
            if (compilationMode is CompilationMode.Incremental) {
                // todo: feels dirty, can this be refactored?
                val dirtySourcesSet = dirtySources.toHashSet()
                val additionalDirtyFiles = additionalDirtyFiles(caches, generatedFiles, services).filter { it !in dirtySourcesSet }
                if (additionalDirtyFiles.isNotEmpty()) {
                    dirtySources.addAll(additionalDirtyFiles)
                    generatedFiles.forEach { it.outputFile.delete() }
                    continue
                }
            }

            reporter.reportCompileIteration(compilationMode is CompilationMode.Incremental, sourcesToCompile, exitCode)
            bufferingMessageCollector.flush(originalMessageCollector)

            if (exitCode != ExitCode.OK) break

            dirtySourcesSinceLastTimeFile.delete()

            val changesCollector = ChangesCollector()
            reporter.measure(BuildTime.IC_UPDATE_CACHES) {
                caches.platformCache.updateComplementaryFiles(dirtySources, expectActualTracker)
                caches.inputsCache.registerOutputForSourceFiles(generatedFiles)
                caches.lookupCache.update(lookupTracker, sourcesToCompile, removedKotlinSources)
                updateCaches(services, caches, generatedFiles, changesCollector)
            }
            if (compilationMode is CompilationMode.Rebuild) {
                if (withSnapshot) {
                    abiSnapshot.protos.putAll(changesCollector.protoDataChanges())
                }
                break
            }

            val (dirtyLookupSymbols, dirtyClassFqNames, forceRecompile) = changesCollector.getDirtyData(listOf(caches.platformCache), reporter)
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
            }

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)

            //update
            if (withSnapshot) {
                //TODO(valtman) check method/ kts class remove
                changesCollector.protoDataRemoved().forEach { abiSnapshot.protos.remove(it) }
                abiSnapshot.protos.putAll(changesCollector.protoDataChanges())
            }
        }

        if (exitCode == ExitCode.OK) {
            reporter.measure(BuildTime.STORE_BUILD_INFO) {
                BuildInfo.write(currentBuildInfo, lastBuildInfoFile)

                //write abi snapshot
                if (withSnapshot) {
                    //TODO(valtman) check method/class remove
                    AbiSnapshotImpl.write(abiSnapshot, abiSnapshotFile)
                }
            }
        }
        if (exitCode == ExitCode.OK && compilationMode is CompilationMode.Incremental) {
            buildDirtyLookupSymbols.addAll(additionalDirtyLookupSymbols())
        }

        val dirtyData = DirtyData(buildDirtyLookupSymbols, buildDirtyFqNames)
        processChangesAfterBuild(compilationMode, currentBuildInfo, dirtyData)

        return exitCode
    }

    protected fun getRemovedClassesChanges(
        caches: IncrementalCachesManager<*>,
        changedFiles: ChangedFiles.Known
    ): DirtyData {
        val removedClasses = HashSet<String>()
        val dirtyFiles = changedFiles.modified.filterTo(HashSet()) { it.isKotlinFile(kotlinSourceFilesExtensions) }
        val removedFiles = changedFiles.removed.filterTo(HashSet()) { it.isKotlinFile(kotlinSourceFilesExtensions) }

        val existingClasses = classesFqNames(dirtyFiles)
        val previousClasses = caches.platformCache
            .classesFqNamesBySources(dirtyFiles + removedFiles)
            .map { it.asString() }

        for (fqName in previousClasses) {
            if (fqName !in existingClasses) {
                removedClasses.add(fqName)
            }
        }

        val changesCollector = ChangesCollector()
        removedClasses.forEach { changesCollector.collectSignature(FqName(it), areSubclassesAffected = true) }
        return changesCollector.getDirtyData(listOf(caches.platformCache), reporter)
    }

    open fun runWithNoDirtyKotlinSources(caches: CacheManager): Boolean = false

    private fun processChangesAfterBuild(
        compilationMode: CompilationMode,
        currentBuildInfo: BuildInfo,
        dirtyData: DirtyData
    ) = reporter.measure(BuildTime.IC_WRITE_HISTORY_FILE) {
        val prevDiffs = BuildDiffsStorage.readFromFile(buildHistoryFile, reporter)?.buildDiffs ?: emptyList()
        val newDiff = if (compilationMode is CompilationMode.Incremental) {
            BuildDifference(currentBuildInfo.startTS, true, dirtyData)
        } else {
            val emptyDirtyData = DirtyData()
            BuildDifference(currentBuildInfo.startTS, false, emptyDirtyData)
        }

        //TODO(valtman) old history build should be restored in case of build fail
        BuildDiffsStorage.writeToFile(buildHistoryFile, BuildDiffsStorage(prevDiffs + newDiff), reporter)
    }

    /**
     * Performs some work after a compilation if the compilation completed successfully.
     *
     * This method MUST NOT be called when the compilation failed because the results produced by the work here would be incorrect.
     */
    protected open fun performWorkAfterSuccessfulCompilation(caches: CacheManager) {}

    companion object {
        const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
        const val LAST_BUILD_INFO_FILE_NAME = "last-build.bin"
        const val ABI_SNAPSHOT_FILE_NAME = "abi-snapshot.bin"
    }

    private object EmptyCompilationCanceledStatus : CompilationCanceledStatus {
        override fun checkCanceled() {
        }
    }

    protected fun reportPerformanceData(defaultPerformanceManager: CommonCompilerPerformanceManager) {
        defaultPerformanceManager.getMeasurementResults().forEach {
            when (it) {
                is CompilerInitializationMeasurement -> reporter.addTimeMetricMs(BuildTime.COMPILER_INITIALIZATION, it.milliseconds)
                is CodeAnalysisMeasurement -> reporter.addTimeMetricMs(BuildTime.CODE_ANALYSIS, it.milliseconds)
                is CodeGenerationMeasurement -> reporter.addTimeMetricMs(BuildTime.CODE_GENERATION, it.milliseconds)
            }
        }
    }
}

