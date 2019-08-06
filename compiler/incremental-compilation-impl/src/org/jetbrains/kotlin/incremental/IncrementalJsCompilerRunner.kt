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
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.isIrBackendEnabled
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalDataProviderFromCache
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import java.io.File

fun makeJsIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JSCompilerArguments,
    messageCollector: MessageCollector = MessageCollector.NONE,
    reporter: ICReporter = EmptyICReporter
) {
    val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
        .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()
    val buildHistoryFile = File(cachesDir, "build-history.bin")

    withJsIC {
        val compiler = IncrementalJsCompilerRunner(
            cachesDir, reporter,
            buildHistoryFile = buildHistoryFile,
            modulesApiHistory = EmptyModulesApiHistory
        )
        compiler.compile(allKotlinFiles, args, messageCollector, providedChangedFiles = null)
    }
}

inline fun <R> withJsIC(fn: () -> R): R {
    val isJsEnabledBackup = IncrementalCompilation.isEnabledForJs()
    IncrementalCompilation.setIsEnabledForJs(true)

    try {
        return fn()
    } finally {
        IncrementalCompilation.setIsEnabledForJs(isJsEnabledBackup)
    }
}

class IncrementalJsCompilerRunner(
    workingDir: File,
    reporter: ICReporter,
    buildHistoryFile: File,
    private val modulesApiHistory: ModulesApiHistory
) : IncrementalCompilerRunner<K2JSCompilerArguments, IncrementalJsCachesManager>(
    workingDir,
    "caches-js",
    reporter,
    buildHistoryFile = buildHistoryFile
) {
    override fun isICEnabled(): Boolean =
        IncrementalCompilation.isEnabledForJs()

    override fun createCacheManager(args: K2JSCompilerArguments): IncrementalJsCachesManager {
        val serializerProtocol = if (!args.isIrBackendEnabled()) JsSerializerProtocol else KlibMetadataSerializerProtocol
        return IncrementalJsCachesManager(cacheDirectory, reporter, serializerProtocol)
    }

    override fun destinationDir(args: K2JSCompilerArguments): File =
        File(args.outputFile).parentFile

    override fun calculateSourcesToCompile(
        caches: IncrementalJsCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JSCompilerArguments
    ): CompilationMode {
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
            ?: return CompilationMode.Rebuild { "No information on previous build" }

        val dirtyFiles = DirtyFilesContainer(caches, reporter, kotlinSourceFilesExtensions)
        initDirtyFiles(dirtyFiles, changedFiles)

        val libs = (args.libraries ?: "").split(File.pathSeparator).map { File(it) }
        val classpathChanges = getClasspathChanges(libs, changedFiles, lastBuildInfo, modulesApiHistory, reporter)

        @Suppress("UNUSED_VARIABLE") // for sealed when
        val unused = when (classpathChanges) {
            is ChangesEither.Unknown -> return CompilationMode.Rebuild {
                // todo: we can recompile all files incrementally (not cleaning caches), so rebuild won't propagate
                "Could not get classpath's changes${classpathChanges.reason?.let { ": $it" }}"
            }
            is ChangesEither.Known -> {
                dirtyFiles.addByDirtySymbols(classpathChanges.lookupSymbols)
                dirtyFiles.addByDirtyClasses(classpathChanges.fqNames)
            }
        }


        val removedClassesChanges = getRemovedClassesChanges(caches, changedFiles)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)

        return CompilationMode.Incremental(dirtyFiles)
    }

    override fun makeServices(
        args: K2JSCompilerArguments,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        caches: IncrementalJsCachesManager,
        compilationMode: CompilationMode
    ): Services.Builder =
        super.makeServices(args, lookupTracker, expectActualTracker, caches, compilationMode).apply {
            register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())

            if (compilationMode is CompilationMode.Incremental) {
                register(
                    IncrementalDataProvider::class.java,
                    IncrementalDataProviderFromCache(caches.platformCache)
                )
            }
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

        return try {
            args.freeArgs += sourcesToCompile.map { it.absolutePath }
            K2JSCompiler().exec(messageCollector, services, args)
        } finally {
            args.freeArgs = freeArgsBackup
        }
    }
}