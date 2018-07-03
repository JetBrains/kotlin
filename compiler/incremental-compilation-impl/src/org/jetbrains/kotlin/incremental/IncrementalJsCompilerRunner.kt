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

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.*
import java.io.File

fun makeJsIncrementally(
        cachesDir: File,
        sourceRoots: Iterable<File>,
        args: K2JSCompilerArguments,
        messageCollector: MessageCollector = MessageCollector.NONE,
        reporter: ICReporter = EmptyICReporter
) {
    val isIncremental = IncrementalCompilation.isEnabledForJs()
    val versions = commonCacheVersions(cachesDir, isIncremental) + standaloneCacheVersion(cachesDir, isIncremental)
    val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
            .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

    withJsIC {
        val compiler = IncrementalJsCompilerRunner(cachesDir, versions, reporter)
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
        cacheVersions: List<CacheVersion>,
        reporter: ICReporter
) : IncrementalCompilerRunner<K2JSCompilerArguments, IncrementalJsCachesManager>(
        workingDir,
        "caches-js",
        cacheVersions,
        reporter
) {
    override fun isICEnabled(): Boolean =
        IncrementalCompilation.isEnabledForJs()

    override fun createCacheManager(args: K2JSCompilerArguments): IncrementalJsCachesManager =
        IncrementalJsCachesManager(cacheDirectory, reporter)

    override fun destinationDir(args: K2JSCompilerArguments): File =
        File(args.outputFile).parentFile

    override fun calculateSourcesToCompile(caches: IncrementalJsCachesManager, changedFiles: ChangedFiles.Known, args: K2JSCompilerArguments): CompilationMode {
        if (BuildInfo.read(lastBuildInfoFile) == null) return CompilationMode.Rebuild { "No information on previous build" }

        val libs = (args.libraries ?: "").split(File.pathSeparator).mapTo(HashSet()) { File(it) }
        val libsDirs = libs.filter { it.isDirectory }

        val changedLib = changedFiles.allAsSequence.find { it in libs }
                         ?: changedFiles.allAsSequence.find { changedFile ->
                                libsDirs.any { libDir -> FileUtil.isAncestor(libDir, changedFile, true) }
                            }

        if (changedLib != null) return CompilationMode.Rebuild { "Library has been changed: $changedLib" }

        val dirtyFiles = DirtyFilesContainer(caches, reporter)
        initDirtyFiles(dirtyFiles, changedFiles)

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
        val incrementalResults = services.get(IncrementalResultsConsumer::class.java) as IncrementalResultsConsumerImpl

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

        try {
            args.freeArgs += sourcesToCompile.map() { it.absolutePath }
            val exitCode = K2JSCompiler().exec(messageCollector, services, args)
            reporter.reportCompileIteration(sourcesToCompile, exitCode)
            return exitCode
        }
        finally {
            args.freeArgs = freeArgsBackup
        }
    }

    private val ChangedFiles.Known.allAsSequence: Sequence<File>
        get() = modified.asSequence() + removed.asSequence()
}