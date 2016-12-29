/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import com.intellij.util.io.PersistentEnumeratorBase
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import org.jetbrains.kotlin.incremental.multiproject.ChangesRegistry
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.io.IOException
import java.util.*

fun makeIncrementally(
        cachesDir: File,
        sourceRoots: Iterable<File>,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector = MessageCollector.NONE,
        reporter: ICReporter = EmptyICReporter
) {
    val versions = commonCacheVersions(cachesDir) + standaloneCacheVersion(cachesDir)

    val kotlinExtensions = listOf("kt", "kts")
    val allExtensions = kotlinExtensions + listOf("java")
    val rootsWalk = sourceRoots.asSequence().flatMap { it.walk() }
    val files = rootsWalk.filter(File::isFile)
    val sourceFiles = files.filter { it.extension.toLowerCase() in allExtensions }.toList()
    val kotlinFiles = sourceFiles.filter { it.extension.toLowerCase() in kotlinExtensions }

    withIC {
        val compiler = IncrementalJvmCompilerRunner(cachesDir, /* javaSourceRoots = */sourceRoots.toSet(), versions, reporter)
        compiler.compile(kotlinFiles, args, messageCollector) {
            it.incrementalCache.sourceSnapshotMap.compareAndUpdate(sourceFiles)
        }
    }
}

private object EmptyICReporter : ICReporter {
    override fun report(message: ()->String) {
    }
}

private inline fun withIC(fn: ()->Unit) {
    val isEnabledBackup = IncrementalCompilation.isEnabled()
    val isExperimentalBackup = IncrementalCompilation.isExperimental()
    IncrementalCompilation.setIsEnabled(true)
    IncrementalCompilation.setIsExperimental(true)

    try {
        fn()
    }
    finally {
        IncrementalCompilation.setIsEnabled(isEnabledBackup)
        IncrementalCompilation.setIsExperimental(isExperimentalBackup)
    }
}

class IncrementalJvmCompilerRunner(
        workingDir: File,
        private val javaSourceRoots: Set<File>,
        private val cacheVersions: List<CacheVersion>,
        private val reporter: ICReporter,
        private var kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null,
        private val artifactChangesProvider: ArtifactChangesProvider? = null,
        private val changesRegistry: ChangesRegistry? = null
) {
    var anyClassesCompiled: Boolean = false
            private set
    private val cacheDirectory = File(workingDir, CACHES_DIR_NAME)
    private val dirtySourcesSinceLastTimeFile = File(workingDir, DIRTY_SOURCES_FILE_NAME)
    private val lastBuildInfoFile = File(workingDir, LAST_BUILD_INFO_FILE_NAME)

    fun compile(
            allKotlinSources: List<File>,
            args: K2JVMCompilerArguments,
            messageCollector: MessageCollector,
            getChangedFiles: (IncrementalCachesManager)->ChangedFiles
    ): ExitCode {
        val targetId = TargetId(name = args.moduleName, type = "java-production")
        var caches = IncrementalCachesManager(targetId, cacheDirectory, File(args.destination), reporter)

        fun onError(e: Exception): ExitCode {
            caches.clean()

            // todo: warn?
            reporter.report { "Possible cache corruption. Rebuilding. $e" }
            // try to rebuild
            val javaFilesProcessor = ChangedJavaFilesProcessor(reporter)
            caches = IncrementalCachesManager(targetId, cacheDirectory, args.destinationAsFile, reporter)
            return compileIncrementally(args, caches, javaFilesProcessor, allKotlinSources, targetId, CompilationMode.Rebuild, messageCollector)
        }

        return try {
            val javaFilesProcessor = ChangedJavaFilesProcessor(reporter)
            val changedFiles = getChangedFiles(caches)
            val compilationMode = calculateSourcesToCompile(javaFilesProcessor, caches, changedFiles, args)
            compileIncrementally(args, caches, javaFilesProcessor, allKotlinSources, targetId, compilationMode, messageCollector)
        }
        catch (e: PersistentEnumeratorBase.CorruptedException) {
            onError(e)
        }
        catch (e: IOException) {
            onError(e)
        }
        finally {
            caches.close(flush = true)
            reporter.report { "flushed incremental caches" }
        }
    }

    private data class CompileChangedResults(val exitCode: ExitCode, val generatedFiles: List<GeneratedFile<TargetId>>)

    private sealed class CompilationMode {
        class Incremental(val dirtyFiles: Set<File>) : CompilationMode()
        object Rebuild : CompilationMode()
    }

    private fun calculateSourcesToCompile(
            javaFilesProcessor: ChangedJavaFilesProcessor,
            caches: IncrementalCachesManager,
            changedFiles: ChangedFiles,
            args: K2JVMCompilerArguments
    ): CompilationMode {
        fun rebuild(reason: ()->String): CompilationMode {
            reporter.report {"Non-incremental compilation will be performed: ${reason()}"}
            caches.clean()
            dirtySourcesSinceLastTimeFile.delete()
            args.destinationAsFile.deleteRecursively()
            return CompilationMode.Rebuild
        }

        if (changedFiles !is ChangedFiles.Known) return rebuild {"inputs' changes are unknown (first or clean build)"}

        val removedClassFiles = changedFiles.removed.filter(File::isClassFile)
        if (removedClassFiles.any()) return rebuild {"Removed class files: ${reporter.pathsAsString(removedClassFiles)}"}

        val modifiedClassFiles = changedFiles.modified.filter(File::isClassFile)
        if (modifiedClassFiles.any()) return rebuild {"Modified class files: ${reporter.pathsAsString(modifiedClassFiles)}"}

        val classpathSet = args.classpathAsList.toHashSet()
        val modifiedClasspathEntries = changedFiles.modified.filter {it in classpathSet}
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
        reporter.report { "Last Kotlin Build info -- $lastBuildInfo" }
        val classpathChanges = getClasspathChanges(modifiedClasspathEntries, lastBuildInfo)
        if (classpathChanges !is ChangesEither.Known) {
            return rebuild {"could not get changes from modified classpath entries: ${reporter.pathsAsString(modifiedClasspathEntries)}"}
        }

        val javaFilesChanges = javaFilesProcessor.process(changedFiles)
        val affectedJavaSymbols = when (javaFilesChanges) {
            is ChangesEither.Known -> javaFilesChanges.lookupSymbols
            is ChangesEither.Unknown -> return rebuild {"Could not get changes for java files"}
        }

        val dirtyFiles = HashSet<File>(with(changedFiles) {modified.size + removed.size})
        with(changedFiles) {
            modified.asSequence() + removed.asSequence()
        }.forEach {if (it.isKotlinFile()) dirtyFiles.add(it)}

        val lookupSymbols = HashSet<LookupSymbol>()
        lookupSymbols.addAll(affectedJavaSymbols)
        lookupSymbols.addAll(classpathChanges.lookupSymbols)

        if (lookupSymbols.any()) {
            val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, reporter)
            dirtyFiles.addAll(dirtyFilesFromLookups)
        }

        val dirtyClassesFqNames = classpathChanges.fqNames.flatMap {withSubtypes(it, listOf(caches.incrementalCache))}
        if (dirtyClassesFqNames.any()) {
            val dirtyFilesFromFqNames = mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassesFqNames, reporter)
            dirtyFiles.addAll(dirtyFilesFromFqNames)
        }

        if (dirtySourcesSinceLastTimeFile.exists()) {
            val files = dirtySourcesSinceLastTimeFile.readLines().map(::File).filter(File::exists)
            if (files.isNotEmpty()) {
                reporter.report {"Source files added since last compilation: ${reporter.pathsAsString(files)}"}
            }

            dirtyFiles.addAll(files)
        }

        return CompilationMode.Incremental(dirtyFiles)
    }

    private fun getClasspathChanges(
            modifiedClasspath: List<File>,
            lastBuildInfo: BuildInfo?
    ): ChangesEither {
        if (modifiedClasspath.isEmpty()) {
            reporter.report {"No classpath changes"}
            return ChangesEither.Known()
        }

        val lastBuildTS = lastBuildInfo?.startTS
        if (lastBuildTS == null) {
            reporter.report {"Could not determine last build timestamp"}
            return ChangesEither.Unknown()
        }

        val symbols = HashSet<LookupSymbol>()
        val fqNames = HashSet<FqName>()
        for (file in modifiedClasspath) {
            val diffs = artifactChangesProvider?.getChanges(file, lastBuildTS)

            if (diffs == null) {
                reporter.report {"Could not get changes for file: $file"}
                return ChangesEither.Unknown()
            }

            diffs.forEach {
                symbols.addAll(it.dirtyLookupSymbols)
                fqNames.addAll(it.dirtyClassesFqNames)
            }
        }

        return ChangesEither.Known(symbols, fqNames)
    }

    private fun compileIncrementally(
            args: K2JVMCompilerArguments,
            caches: IncrementalCachesManager,
            javaFilesProcessor: ChangedJavaFilesProcessor,
            allKotlinSources: List<File>,
            targetId: TargetId,
            compilationMode: CompilationMode,
            messageCollector: MessageCollector
    ): ExitCode {
        assert(IncrementalCompilation.isEnabled()) { "Incremental compilation is not enabled" }
        assert(IncrementalCompilation.isExperimental()) { "Experimental incremental compilation is not enabled" }

        val allGeneratedFiles = hashSetOf<GeneratedFile<TargetId>>()
        val dirtySources: MutableList<File>

        when (compilationMode) {
            is CompilationMode.Incremental -> {
                dirtySources = ArrayList(compilationMode.dirtyFiles)
                args.classpathAsList += args.destinationAsFile
            }
            is CompilationMode.Rebuild -> {
                dirtySources = allKotlinSources.toMutableList()
                // there is no point in updating annotation file since all files will be compiled anyway
                kaptAnnotationsFileUpdater = null
            }
            else -> throw IllegalStateException("Unknown CompilationMode ${compilationMode.javaClass}")
        }

        @Suppress("NAME_SHADOWING")
        var compilationMode = compilationMode

        val currentBuildInfo = BuildInfo(startTS = System.currentTimeMillis())
        BuildInfo.write(currentBuildInfo, lastBuildInfoFile)
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()
        val allSourcesToCompile = HashSet<File>()

        var exitCode = ExitCode.OK
        while (dirtySources.any()) {
            val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
            val outdatedClasses = caches.incrementalCache.classesBySources(dirtySources)
            caches.incrementalCache.markOutputClassesDirty(dirtySources)
            caches.incrementalCache.removeClassfilesBySources(dirtySources)

            val (sourcesToCompile, removedKotlinSources) = dirtySources.partition(File::exists)

            // todo: more optimal to save only last iteration, but it will require adding standalone-ic specific logs
            // (because jps rebuilds all files from last build if it failed and gradle rebuilds everything)
            allSourcesToCompile.addAll(sourcesToCompile)
            val text = allSourcesToCompile.map { it.canonicalPath }.joinToString(separator = System.getProperty("line.separator"))
            dirtySourcesSinceLastTimeFile.writeText(text)

            val compilerOutput = compileChanged(listOf(targetId), sourcesToCompile.toSet(), args, { caches.incrementalCache }, lookupTracker, messageCollector)
            exitCode = compilerOutput.exitCode
            val generatedClassFiles = compilerOutput.generatedFiles
            anyClassesCompiled = anyClassesCompiled || generatedClassFiles.isNotEmpty() || removedKotlinSources.isNotEmpty()

            if (exitCode == ExitCode.OK) {
                dirtySourcesSinceLastTimeFile.delete()
                kaptAnnotationsFileUpdater?.updateAnnotations(outdatedClasses)
            } else {
                kaptAnnotationsFileUpdater?.revert()
                break
            }

            if (compilationMode is CompilationMode.Incremental) {
                val dirtySourcesSet = dirtySources.toHashSet()
                val additionalDirtyFiles = additionalDirtyFiles(caches, generatedClassFiles).filter { it !in dirtySourcesSet }
                if (additionalDirtyFiles.isNotEmpty()) {
                    dirtySources.addAll(additionalDirtyFiles)
                    continue
                }
            }

            allGeneratedFiles.addAll(generatedClassFiles)
            val compilationResult = updateIncrementalCaches(listOf(targetId), generatedClassFiles,
                    compiledWithErrors = exitCode != ExitCode.OK,
                    getIncrementalCache = { caches.incrementalCache })

            caches.lookupCache.update(lookupTracker, sourcesToCompile, removedKotlinSources)

            if (compilationMode is CompilationMode.Rebuild) {
                break
            }

            val (dirtyLookupSymbols, dirtyClassFqNames) = compilationResult.getDirtyData(listOf(caches.incrementalCache), reporter)
            val compiledInThisIterationSet = sourcesToCompile.toHashSet()

            with (dirtySources) {
                clear()
                addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = compiledInThisIterationSet))
                addAll(mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassFqNames, reporter, excludes = compiledInThisIterationSet))
            }

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)
        }

        if (exitCode == ExitCode.OK && compilationMode is CompilationMode.Incremental) {
            buildDirtyLookupSymbols.addAll(javaFilesProcessor.allChangedSymbols)
        }
        if (changesRegistry != null) {
            if (compilationMode is CompilationMode.Incremental) {
                val dirtyData = DirtyData(buildDirtyLookupSymbols, buildDirtyFqNames)
                changesRegistry.registerChanges(currentBuildInfo.startTS, dirtyData)
            }
            else {
                assert(compilationMode is CompilationMode.Rebuild) { "Unexpected compilation mode: ${compilationMode.javaClass}" }
                changesRegistry.unknownChanges(currentBuildInfo.startTS)
            }
        }

        if (exitCode == ExitCode.OK) {
            cacheVersions.forEach { it.saveIfNeeded() }
        }

        return exitCode
    }

    private fun additionalDirtyFiles(
            caches: IncrementalCachesManager,
            generatedFiles: List<GeneratedFile<TargetId>>
    ): Collection<File> {
        val result = HashSet<File>()

        fun partsByFacadeName(facadeInternalName: String): List<File> {
            val parts = caches.incrementalCache.getStableMultifileFacadeParts(facadeInternalName) ?: emptyList()
            return parts.flatMap { caches.incrementalCache.sourcesByInternalName(it) }
        }

        for (generatedFile in generatedFiles) {
            if (generatedFile !is GeneratedJvmClass<*>) continue

            val outputClass = generatedFile.outputClass

            when (outputClass.classHeader.kind) {
                KotlinClassHeader.Kind.CLASS -> {
                    val fqName = outputClass.className.fqNameForClassNameWithoutDollars
                    val cachedSourceFile = caches.incrementalCache.getSourceFileIfClass(fqName)

                    if (cachedSourceFile != null) {
                        result.add(cachedSourceFile)
                    }
                }
                // todo: more optimal is to check if public API or parts list changed
                KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
                    result.addAll(partsByFacadeName(outputClass.className.internalName))
                }
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    result.addAll(partsByFacadeName(outputClass.classHeader.multifileClassName!!))
                }

            }
        }

        return result
    }

    private fun compileChanged(
            targets: List<TargetId>,
            sourcesToCompile: Set<File>,
            args: K2JVMCompilerArguments,
            getIncrementalCache: (TargetId)->GradleIncrementalCacheImpl,
            lookupTracker: LookupTracker,
            messageCollector: MessageCollector
    ): CompileChangedResults {
        val compiler = K2JVMCompiler()
        val outputDir = args.destinationAsFile
        val classpath = args.classpathAsList
        val moduleFile = makeModuleFile(args.moduleName,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = javaSourceRoots,
                classpath = classpath,
                friendDirs = listOf())
        args.module = moduleFile.absolutePath
        val outputItemCollector = OutputItemsCollectorImpl()
        @Suppress("NAME_SHADOWING")
        val messageCollector = MessageCollectorWrapper(messageCollector, outputItemCollector)

        try {
            val incrementalCaches = makeIncrementalCachesMap(targets, { listOf<TargetId>() }, getIncrementalCache, { this })
            val compilationCanceledStatus = object : CompilationCanceledStatus {
                override fun checkCanceled() {
                }
            }

            reporter.report { "compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}" }
            reporter.report { "compiling with classpath: ${classpath.toList().sorted().joinToString()}" }
            val compileServices = makeCompileServices(incrementalCaches, lookupTracker, compilationCanceledStatus)
            val exitCode = compiler.exec(messageCollector, compileServices, args)
            val generatedFiles = outputItemCollector.generatedFiles(targets, targets.first(), {sourcesToCompile}, {outputDir})
            reporter.reportCompileIteration(sourcesToCompile, exitCode)
            return CompileChangedResults(exitCode, generatedFiles)
        }
        finally {
            moduleFile.delete()
        }
    }

    private class MessageCollectorWrapper(
            private val delegate: MessageCollector,
            private val outputCollector: OutputItemsCollector
    ) : MessageCollector by delegate {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            // TODO: consider adding some other way of passing input -> output mapping from compiler, e.g. dedicated service
            OutputMessageUtil.parseOutputMessage(message)?.let {
                outputCollector.add(it.sourceFiles, it.outputFile)
            }
            delegate.report(severity, message, location)
        }
    }

    companion object {
        const val CACHES_DIR_NAME = "caches"
        const val DIRTY_SOURCES_FILE_NAME = "dirty-sources.txt"
        const val LAST_BUILD_INFO_FILE_NAME = "last-build.bin"
    }
}

var K2JVMCompilerArguments.destinationAsFile: File
        get() = File(destination)
        set(value) { destination = value.path }

var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath.split(File.pathSeparator).map(::File)
    set(value) { classpath = value.joinToString(separator = File.pathSeparator, transform = { it.path }) }