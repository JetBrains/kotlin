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
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.MessageCollectorToOutputItemsCollectorAdapter
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.SimpleOutputItem
import org.jetbrains.kotlin.compilerRunner.toGeneratedFile
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.parsing.classesFqNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.util.*

abstract class IncrementalCompilerRunner<
        Args : CommonCompilerArguments,
        CacheManager : IncrementalCachesManager<*>
        >(
    private val workingDir: File,
    cacheDirName: String,
    protected val reporter: ICReporter,
    private val buildHistoryFile: File,
    // there might be some additional output directories (e.g. for generated java in kapt)
    // to remove them correctly on rebuild, we pass them as additional argument
    private val outputFiles: Collection<File> = emptyList()
) {

    protected val cacheDirectory = File(workingDir, cacheDirName)
    private val dirtySourcesSinceLastTimeFile = File(workingDir, DIRTY_SOURCES_FILE_NAME)
    protected val lastBuildInfoFile = File(workingDir, LAST_BUILD_INFO_FILE_NAME)
    protected open val kotlinSourceFilesExtensions: List<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS

    protected abstract fun isICEnabled(): Boolean
    protected abstract fun createCacheManager(args: Args): CacheManager
    protected abstract fun destinationDir(args: Args): File

    fun compile(
        allSourceFiles: List<File>,
        args: Args,
        messageCollector: MessageCollector,
        // when [providedChangedFiles] is not null, changes are provided by external system (e.g. Gradle)
        // otherwise we track source files changes ourselves.
        providedChangedFiles: ChangedFiles?
    ): ExitCode {
        assert(isICEnabled()) { "Incremental compilation is not enabled" }
        var caches = createCacheManager(args)

        fun rebuild(reason: () -> String): ExitCode {
            reporter.report(reason)
            caches.close(false)
            clearLocalStateOnRebuild(args)
            caches = createCacheManager(args)
            if (providedChangedFiles == null) {
                caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
            }
            val allKotlinFiles = allSourceFiles.filter { it.isKotlinFile(kotlinSourceFilesExtensions) }
            return compileIncrementally(args, caches, allKotlinFiles, CompilationMode.Rebuild(), messageCollector)
        }

        return try {
            val changedFiles = providedChangedFiles ?: caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allSourceFiles)
            val compilationMode = sourcesToCompile(caches, changedFiles, args)

            val exitCode = when (compilationMode) {
                is CompilationMode.Incremental -> {
                    compileIncrementally(args, caches, allSourceFiles, compilationMode, messageCollector)
                }
                is CompilationMode.Rebuild -> {
                    rebuild { "Non-incremental compilation will be performed: ${compilationMode.reason}" }
                }
            }

            if (!caches.close(flush = true)) throw RuntimeException("Could not flush caches")

            return exitCode
        } catch (e: Exception) {
            // todo: warn?
            rebuild { "Possible cache corruption. Rebuilding. $e" }
        }
    }

    private fun clearLocalStateOnRebuild(args: Args) {
        val destinationDir = destinationDir(args)

        reporter.reportVerbose { "Clearing output on rebuild" }
        for (file in sequenceOf(destinationDir, workingDir) + outputFiles.asSequence()) {
            val deleted: Boolean? = when {
                file.isDirectory -> {
                    reporter.reportVerbose { "  Deleting directory $file" }
                    file.deleteRecursively()
                }
                file.isFile -> {
                    reporter.reportVerbose { "  Deleting $file" }
                    file.delete()
                }
                else -> null
            }

            if (deleted == false) {
                reporter.reportVerbose { "  Could not delete $file" }
            }
        }

        assert(!destinationDir.exists()) { "Could not delete destination dir $destinationDir" }
        assert(!workingDir.exists()) { "Could not delete caches dir $workingDir" }
        destinationDir.mkdirs()
        workingDir.mkdirs()
    }

    private fun sourcesToCompile(caches: CacheManager, changedFiles: ChangedFiles, args: Args): CompilationMode =
        when (changedFiles) {
            is ChangedFiles.Known -> calculateSourcesToCompile(caches, changedFiles, args)
            is ChangedFiles.Unknown -> CompilationMode.Rebuild { "inputs' changes are unknown (first or clean build)" }
        }

    protected abstract fun calculateSourcesToCompile(caches: CacheManager, changedFiles: ChangedFiles.Known, args: Args): CompilationMode

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
        class Rebuild(getReason: () -> String = { "" }) : CompilationMode() {
            val reason: String by lazy(getReason)
        }
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
        messageCollector: MessageCollector
    ): ExitCode {
        preBuildHook(args, compilationMode)

        val dirtySources = when (compilationMode) {
            is CompilationMode.Incremental -> compilationMode.dirtyFiles.toMutableList()
            is CompilationMode.Rebuild -> allKotlinSources.toMutableList()
        }

        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis())
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
            val temporaryMessageCollector = TemporaryMessageCollector(messageCollector)
            val messageCollectorAdapter = MessageCollectorToOutputItemsCollectorAdapter(temporaryMessageCollector, outputItemsCollector)

            exitCode = runCompiler(sourcesToCompile.toSet(), args, caches, services, messageCollectorAdapter)

            val generatedFiles = outputItemsCollector.outputs.map(SimpleOutputItem::toGeneratedFile)
            if (compilationMode is CompilationMode.Incremental) {
                // todo: feels dirty, can this be refactored?
                val dirtySourcesSet = dirtySources.toHashSet()
                val additionalDirtyFiles = additionalDirtyFiles(caches, generatedFiles, services).filter { it !in dirtySourcesSet }
                if (additionalDirtyFiles.isNotEmpty()) {
                    dirtySources.addAll(additionalDirtyFiles)
                    continue
                }
            }

            reporter.reportCompileIteration(compilationMode is CompilationMode.Incremental, sourcesToCompile, exitCode)
            temporaryMessageCollector.flush()

            if (exitCode != ExitCode.OK) break

            dirtySourcesSinceLastTimeFile.delete()

            caches.platformCache.updateComplementaryFiles(dirtySources, expectActualTracker)
            caches.inputsCache.registerOutputForSourceFiles(generatedFiles)
            caches.lookupCache.update(lookupTracker, sourcesToCompile, removedKotlinSources)
            val changesCollector = ChangesCollector()
            updateCaches(services, caches, generatedFiles, changesCollector)

            if (compilationMode is CompilationMode.Rebuild) break

            val (dirtyLookupSymbols, dirtyClassFqNames) = changesCollector.getDirtyData(listOf(caches.platformCache), reporter)
            val compiledInThisIterationSet = sourcesToCompile.toHashSet()

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
            }

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)
        }

        if (exitCode == ExitCode.OK) {
            BuildInfo.write(currentBuildInfo, lastBuildInfoFile)
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
    ) {
        val prevDiffs = BuildDiffsStorage.readFromFile(buildHistoryFile, reporter)?.buildDiffs ?: emptyList()
        val newDiff = if (compilationMode is CompilationMode.Incremental) {
            BuildDifference(currentBuildInfo.startTS, true, dirtyData)
        } else {
            val emptyDirtyData = DirtyData()
            BuildDifference(currentBuildInfo.startTS, false, emptyDirtyData)
        }

        BuildDiffsStorage.writeToFile(buildHistoryFile, BuildDiffsStorage(prevDiffs + newDiff), reporter)
    }

    companion object {
        const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
        const val LAST_BUILD_INFO_FILE_NAME = "last-build.bin"
    }

    private object EmptyCompilationCanceledStatus : CompilationCanceledStatus {
        override fun checkCanceled() {
        }
    }
}

private class TemporaryMessageCollector(private val delegate: MessageCollector) : MessageCollector {
    private class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageLocation?)

    private val messages = ArrayList<Message>()

    override fun clear() {
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        messages.add(Message(severity, message, location))
    }

    override fun hasErrors(): Boolean =
        messages.any { it.severity.isError }

    fun flush() {
        messages.forEach { delegate.report(it.severity, it.message, it.location) }
    }
}
