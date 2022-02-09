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
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.isIrBackendEnabled
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.*
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import java.io.File

fun makeJsIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JSCompilerArguments,
    buildHistoryFile: File,
    messageCollector: MessageCollector = MessageCollector.NONE,
    reporter: ICReporter = EmptyICReporter,
    scopeExpansion: CompileScopeExpansionMode = CompileScopeExpansionMode.NEVER,
    modulesApiHistory: ModulesApiHistory = EmptyModulesApiHistory,
    providedChangedFiles: ChangedFiles? = null
) {
    val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
        .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

    val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)
    withJsIC(args) {
        val compiler = IncrementalJsCompilerRunner(
            cachesDir, buildReporter,
            buildHistoryFile = buildHistoryFile,
            modulesApiHistory = modulesApiHistory,
            scopeExpansion = scopeExpansion
        )
        compiler.compile(allKotlinFiles, args, messageCollector, providedChangedFiles)
    }
}

@Suppress("DEPRECATION")
inline fun <R> withJsIC(args: CommonCompilerArguments, enabled: Boolean = true, fn: () -> R): R {
    val isJsEnabledBackup = IncrementalCompilation.isEnabledForJs()
    IncrementalCompilation.setIsEnabledForJs(true)

    try {
        if (args.incrementalCompilation == null) {
            args.incrementalCompilation = enabled
        }
        return fn()
    } finally {
        IncrementalCompilation.setIsEnabledForJs(isJsEnabledBackup)
    }
}

class IncrementalJsCompilerRunner(
    workingDir: File,
    reporter: BuildReporter,
    buildHistoryFile: File,
    private val modulesApiHistory: ModulesApiHistory,
    private val scopeExpansion: CompileScopeExpansionMode = CompileScopeExpansionMode.NEVER
) : IncrementalCompilerRunner<K2JSCompilerArguments, IncrementalJsCachesManager>(
    workingDir,
    "caches-js",
    reporter,
    buildHistoryFile = buildHistoryFile
) {

    override fun createCacheManager(args: K2JSCompilerArguments, projectDir: File?): IncrementalJsCachesManager {
        val serializerProtocol = if (!args.isIrBackendEnabled()) JsSerializerProtocol else KlibMetadataSerializerProtocol
        return IncrementalJsCachesManager(
            cacheDirectory,
            projectDir,
            reporter,
            serializerProtocol,
            storeFullFqNamesInLookupCache = withSnapshot
            )
    }

    override fun destinationDir(args: K2JSCompilerArguments): File {
        val outputFile = File(args.outputFile!!)
        return if (args.isIrBackendEnabled())
            outputFile
        else
            outputFile.parentFile
    }

    override fun calculateSourcesToCompileImpl(
        caches: IncrementalJsCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JSCompilerArguments,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot> //Ignore for now
    ): CompilationMode {
        if (!withSnapshot && !buildHistoryFile.isFile) {
            return CompilationMode.Rebuild(BuildAttribute.NO_BUILD_HISTORY)
        }
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)

        val dirtyFiles = DirtyFilesContainer(caches, reporter, kotlinSourceFilesExtensions)
        initDirtyFiles(dirtyFiles, changedFiles)

        val libs = (args.libraries ?: "").split(File.pathSeparator).map { File(it) }
        //TODO(valtman) check for JS
        val classpathChanges = getClasspathChanges(
            libs, changedFiles, lastBuildInfo, modulesApiHistory, reporter,
            mapOf(), false, caches.platformCache,
            caches.lookupCache.lookupSymbols.map { if (it.scope.isBlank()) it.name else it.scope }.distinct()
        )

        @Suppress("UNUSED_VARIABLE") // for sealed when
        val unused = when (classpathChanges) {
            is ChangesEither.Unknown -> {
                reporter.report { "Could not get classpath's changes: ${classpathChanges.reason}" }
                return CompilationMode.Rebuild(classpathChanges.reason)
            }
            is ChangesEither.Known -> {
                dirtyFiles.addByDirtySymbols(classpathChanges.lookupSymbols)
                dirtyFiles.addByDirtyClasses(classpathChanges.fqNames)
            }
        }

        val removedClassesChanges = getRemovedClassesChanges(caches, changedFiles)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNamesForceRecompile)
        return CompilationMode.Incremental(dirtyFiles)
    }

    override fun makeServices(
        args: K2JSCompilerArguments,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        caches: IncrementalJsCachesManager,
        dirtySources: Set<File>,
        isIncremental: Boolean
    ): Services.Builder =
        super.makeServices(args, lookupTracker, expectActualTracker, caches, dirtySources, isIncremental).apply {
            if (isIncremental) {
                if (scopeExpansion == CompileScopeExpansionMode.ALWAYS) {
                    val nextRoundChecker = IncrementalNextRoundCheckerImpl(caches, dirtySources)
                    register(IncrementalNextRoundChecker::class.java, nextRoundChecker)
                }
                register(IncrementalDataProvider::class.java, IncrementalDataProviderFromCache(caches.platformCache))
            }

            register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())
        }

    override fun updateCaches(
        services: Services,
        caches: IncrementalJsCachesManager,
        generatedFiles: List<GeneratedFile>,
        changesCollector: ChangesCollector
    ) {
        val incrementalResults = services[IncrementalResultsConsumer::class.java] as IncrementalResultsConsumerImpl

        val jsCache = caches.platformCache
        jsCache.header = incrementalResults.headerMetadata

        jsCache.compareAndUpdate(incrementalResults, changesCollector)
        jsCache.clearCacheForRemovedClasses(changesCollector)
    }

    override fun runCompiler(
        sourcesToCompile: Set<File>,
        args: K2JSCompilerArguments,
        caches: IncrementalJsCachesManager,
        services: Services,
        messageCollector: MessageCollector
    ): ExitCode {
        val freeArgsBackup = args.freeArgs

        val compiler = K2JSCompiler()
        return try {
            args.freeArgs += sourcesToCompile.map { it.absolutePath }
            compiler.exec(messageCollector, services, args)
        } finally {
            args.freeArgs = freeArgsBackup
            reportPerformanceData(compiler.defaultPerformanceManager)
        }
    }

    override fun additionalDirtyFiles(
        caches: IncrementalJsCachesManager,
        generatedFiles: List<GeneratedFile>,
        services: Services
    ): Iterable<File> {
        val additionalDirtyFiles: Set<File> =
            when (scopeExpansion) {
                CompileScopeExpansionMode.ALWAYS ->
                    (services[IncrementalNextRoundChecker::class.java] as IncrementalNextRoundCheckerImpl).newDirtySources
                CompileScopeExpansionMode.NEVER ->
                    emptySet()
            }

        return additionalDirtyFiles + super.additionalDirtyFiles(caches, generatedFiles, services)
    }

    inner class IncrementalNextRoundCheckerImpl(
        private val caches: IncrementalJsCachesManager,
        private val sourcesToCompile: Set<File>
    ) : IncrementalNextRoundChecker {
        val newDirtySources = HashSet<File>()

        private val emptyByteArray = ByteArray(0)
        private val translatedFiles = HashMap<File, TranslationResultValue>()

        override fun checkProtoChanges(sourceFile: File, packagePartMetadata: ByteArray) {
            translatedFiles[sourceFile] = TranslationResultValue(packagePartMetadata, emptyByteArray, emptyByteArray)
        }

        override fun shouldGoToNextRound(): Boolean {
            val changesCollector = ChangesCollector()
            // todo: split compare and update (or cache comparing)
            caches.platformCache.compare(translatedFiles, changesCollector)
            val (dirtyLookupSymbols, dirtyClassFqNames) = changesCollector.getDirtyData(listOf(caches.platformCache), reporter)
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