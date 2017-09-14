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

import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.MessageCollectorToOutputItemsCollectorAdapter
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.compilerRunner.SimpleOutputItem
import org.jetbrains.kotlin.compilerRunner.toGeneratedFile
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import org.jetbrains.kotlin.incremental.multiproject.ChangesRegistry
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.util.ArrayList
import java.util.HashSet

abstract class IncrementalCompilerRunner<
    Args : CommonCompilerArguments,
    CacheManager : IncrementalCachesManager<*>
>(
        workingDir: File,
        cacheDirName: String,
        protected val cacheVersions: List<CacheVersion>,
        protected val reporter: ICReporter,
        protected val artifactChangesProvider: ArtifactChangesProvider?,
        protected val changesRegistry: ChangesRegistry?
) {

    protected val cacheDirectory = File(workingDir, cacheDirName)
    protected val dirtySourcesSinceLastTimeFile = File(workingDir, DIRTY_SOURCES_FILE_NAME)
    protected val lastBuildInfoFile = File(workingDir, LAST_BUILD_INFO_FILE_NAME)

    protected abstract fun isICEnabled(): Boolean
    protected abstract fun createCacheManager(args: Args): CacheManager
    protected abstract fun destinationDir(args: Args): File

    fun compile(
            allKotlinSources: List<File>,
            args: Args,
            messageCollector: MessageCollector,
            getChangedFiles: (CacheManager)-> ChangedFiles
    ): ExitCode {
        assert(isICEnabled()) { "Incremental compilation is not enabled" }
        var caches = createCacheManager(args)

        fun rebuild(): ExitCode {
            caches.clean()
            dirtySourcesSinceLastTimeFile.delete()
            destinationDir(args).deleteRecursively()

            caches = createCacheManager(args)
            // todo more optimal fix
            caches.inputsCache.sourceSnapshotMap.compareAndUpdate(allKotlinSources)
            return compileIncrementally(args, caches, allKotlinSources, CompilationMode.Rebuild(), messageCollector)
        }

        return try {
            val changedFiles = getChangedFiles(caches)
            val compilationMode = sourcesToCompile(caches, changedFiles, args)

            val exitCode = when (compilationMode) {
                is CompilationMode.Incremental -> {
                    compileIncrementally(args, caches, allKotlinSources, compilationMode, messageCollector)
                }
                is CompilationMode.Rebuild -> {
                    reporter.report { "Non-incremental compilation will be performed: ${compilationMode.reason}" }
                    rebuild()
                }
            }

            if (!caches.close(flush = true)) throw RuntimeException("Could not flush caches")

            return exitCode
        }
        catch (e: Exception) {
            // todo: warn?
            reporter.report { "Possible cache corruption. Rebuilding. $e" }
            rebuild()
        }
    }

    private fun sourcesToCompile(caches: CacheManager, changedFiles: ChangedFiles, args: Args): CompilationMode =
            when (changedFiles) {
                is ChangedFiles.Known -> calculateSourcesToCompile(caches, changedFiles, args)
                is ChangedFiles.Unknown -> CompilationMode.Rebuild { "inputs' changes are unknown (first or clean build)" }
            }

    protected abstract fun calculateSourcesToCompile(caches: CacheManager, changedFiles: ChangedFiles.Known, args: Args): CompilationMode

    protected fun getDirtyFiles(changedFiles: ChangedFiles.Known): HashSet<File> {
        val dirtyFiles = HashSet<File>(with(changedFiles) { modified.size + removed.size })
        with(changedFiles) {
            modified.asSequence() + removed.asSequence()
        }.filterTo(dirtyFiles, File::isKotlinFile)

        if (dirtySourcesSinceLastTimeFile.exists()) {
            val files = dirtySourcesSinceLastTimeFile.readLines().map(::File).filter(File::exists)
            if (files.isNotEmpty()) {
                reporter.report { "Source files added since last compilation: ${reporter.pathsAsString(files)}" }
            }

            dirtyFiles.addAll(files)
        }

        return dirtyFiles
    }

    protected sealed class CompilationMode {
        class Incremental(val dirtyFiles: Set<File>) : CompilationMode()
        class Rebuild(getReason: ()->String = { "" }) : CompilationMode() {
            val reason: String by lazy(getReason)
        }
    }

    protected open fun markDirty(caches: CacheManager, dirtySources: List<File>) {
        caches.platformCache.markDirty(dirtySources)
    }

    protected abstract fun updateCaches(
            services: Services,
            caches: CacheManager,
            generatedFiles: List<GeneratedFile>,
            changesCollector: ChangesCollector
    )

    protected open fun preBuildHook(args: Args, compilationMode: CompilationMode) {}
    protected open fun postCompilationHook(exitCode: ExitCode) {}
    protected open fun additionalDirtyFiles(caches: CacheManager, generatedFiles: List<GeneratedFile>): Iterable<File> =
            emptyList()
    protected open fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
            emptyList()

    protected open fun makeServices(
            args: Args,
            lookupTracker: LookupTracker,
            caches: CacheManager,
            compilationMode: CompilationMode
    ): Services.Builder =
        Services.Builder().apply {
            register(LookupTracker::class.java, lookupTracker)
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
            is CompilationMode.Incremental -> ArrayList(compilationMode.dirtyFiles)
            is CompilationMode.Rebuild -> allKotlinSources.toMutableList()
        }

        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis())
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()
        val allSourcesToCompile = HashSet<File>()

        var exitCode = ExitCode.OK
        val allGeneratedFiles = hashSetOf<GeneratedFile>()

        while (dirtySources.any()) {
            caches.platformCache.markDirty(dirtySources)
            caches.inputsCache.removeOutputForSourceFiles(dirtySources)

            val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
            val (sourcesToCompile, removedKotlinSources) = dirtySources.partition(File::exists)

            // todo: more optimal to save only last iteration, but it will require adding standalone-ic specific logs
            // (because jps rebuilds all files from last build if it failed and gradle rebuilds everything)
            allSourcesToCompile.addAll(sourcesToCompile)
            val text = allSourcesToCompile.joinToString(separator = System.getProperty("line.separator")) { it.canonicalPath }
            dirtySourcesSinceLastTimeFile.writeText(text)

            val services = makeServices(args, lookupTracker, caches, compilationMode).build()

            args.reportOutputFiles = true
            val outputItemsCollector = OutputItemsCollectorImpl()
            val messageCollectorAdapter = MessageCollectorToOutputItemsCollectorAdapter(messageCollector, outputItemsCollector)

            exitCode = runCompiler(sourcesToCompile.toSet(), args, caches, services, messageCollectorAdapter)
            postCompilationHook(exitCode)

            if (exitCode != ExitCode.OK) break

            dirtySourcesSinceLastTimeFile.delete()
            val generatedFiles = outputItemsCollector.outputs.map(SimpleOutputItem::toGeneratedFile)

            if (compilationMode is CompilationMode.Incremental) {
                // todo: feels dirty, can this be refactored?
                val dirtySourcesSet = dirtySources.toHashSet()
                val additionalDirtyFiles = additionalDirtyFiles(caches, generatedFiles).filter { it !in dirtySourcesSet }
                if (additionalDirtyFiles.isNotEmpty()) {
                    dirtySources.addAll(additionalDirtyFiles)
                    continue
                }
            }

            allGeneratedFiles.addAll(generatedFiles)
            caches.inputsCache.registerOutputForSourceFiles(generatedFiles)
            caches.lookupCache.update(lookupTracker, sourcesToCompile, removedKotlinSources)
            val changesCollector = ChangesCollector()
            updateCaches(services, caches, generatedFiles, changesCollector)

            if (compilationMode is CompilationMode.Rebuild) break

            val (dirtyLookupSymbols, dirtyClassFqNames) = changesCollector.getDirtyData(listOf(caches.platformCache), reporter)
            val compiledInThisIterationSet = sourcesToCompile.toHashSet()

            with (dirtySources) {
                clear()
                addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = compiledInThisIterationSet))
                addAll(mapClassesFqNamesToFiles(listOf(caches.platformCache), dirtyClassFqNames, reporter, excludes = compiledInThisIterationSet))
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
        if (changesRegistry != null) {
            if (compilationMode is CompilationMode.Incremental) {
                val dirtyData = DirtyData(buildDirtyLookupSymbols, buildDirtyFqNames)
                changesRegistry.registerChanges(currentBuildInfo.startTS, dirtyData)
            }
            else {
                assert(compilationMode is CompilationMode.Rebuild) { "Unexpected compilation mode: ${compilationMode::class.java}" }
                changesRegistry.unknownChanges(currentBuildInfo.startTS)
            }
        }

        if (exitCode == ExitCode.OK) {
            cacheVersions.forEach { it.saveIfNeeded() }
        }

        return exitCode
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