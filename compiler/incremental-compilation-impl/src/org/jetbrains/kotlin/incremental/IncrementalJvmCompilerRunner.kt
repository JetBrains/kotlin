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
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import org.jetbrains.kotlin.incremental.multiproject.ChangesRegistry
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
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
        val compiler = IncrementalJvmCompilerRunner(cachesDir,
                                                    sourceRoots.map { JvmSourceRoot(it, null) }.toSet(),
                                                    versions, reporter)
        compiler.compile(kotlinFiles, args, messageCollector) {
            it.inputsCache.sourceSnapshotMap.compareAndUpdate(sourceFiles)
        }
    }
}

private object EmptyICReporter : ICReporter {
    override fun report(message: ()->String) {
    }
}

inline fun <R> withIC(enabled: Boolean = true, fn: ()->R): R {
    val isEnabledBackup = IncrementalCompilation.isEnabled()
    IncrementalCompilation.setIsEnabled(enabled)

    try {
        return fn()
    }
    finally {
        IncrementalCompilation.setIsEnabled(isEnabledBackup)
    }
}

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
    protected abstract fun destionationDir(args: Args): File

    fun compile(
            allKotlinSources: List<File>,
            args: Args,
            messageCollector: MessageCollector,
            getChangedFiles: (CacheManager)->ChangedFiles
    ): ExitCode {
        assert(isICEnabled()) { "Incremental compilation is not enabled" }
        var caches = createCacheManager(args)

        fun rebuild(): ExitCode {
            caches.clean()
            dirtySourcesSinceLastTimeFile.delete()
            destionationDir(args).deleteRecursively()

            caches = createCacheManager(args)
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

    protected open fun calculateSourcesToCompile(caches: CacheManager, changedFiles: ChangedFiles.Known, args: Args): CompilationMode =
            CompilationMode.Incremental(getDirtyFiles(changedFiles))

    protected fun getDirtyFiles(changedFiles: ChangedFiles.Known): HashSet<File> {
        val dirtyFiles = HashSet<File>(with(changedFiles) { modified.size + removed.size })
        with(changedFiles) {
            modified.asSequence() + removed.asSequence()
        }.forEach { if (it.isKotlinFile()) dirtyFiles.add(it) }

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

    protected open fun markOutputDirty(caches: CacheManager, dirtySources: List<File>) {
    }

    protected abstract fun compareAndUpdateCache(caches: CacheManager, generatedFiles: List<GeneratedFile>): CompilationResult

    protected open fun preBuildHook(args: Args, compilationMode: CompilationMode) {}
    protected open fun postCompilationHook(exitCode: ExitCode) {}
    protected open fun additionalDirtyFiles(caches: CacheManager, generatedFiles: List<GeneratedFile>): Iterable<File> =
            emptyList()
    protected open fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
            emptyList()

    protected abstract fun runCompiler(
            sourcesToCompile: Set<File>,
            args: Args,
            caches: CacheManager,
            services: Services.Builder,
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
        BuildInfo.write(currentBuildInfo, lastBuildInfoFile)
        val buildDirtyLookupSymbols = HashSet<LookupSymbol>()
        val buildDirtyFqNames = HashSet<FqName>()
        val allSourcesToCompile = HashSet<File>()

        var exitCode = ExitCode.OK
        val allGeneratedFiles = hashSetOf<GeneratedFile>()

        while (dirtySources.any()) {
            markOutputDirty(caches, dirtySources)
            caches.inputsCache.removeOutputForSourceFiles(dirtySources)

            val lookupTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
            val (sourcesToCompile, removedKotlinSources) = dirtySources.partition(File::exists)

            // todo: more optimal to save only last iteration, but it will require adding standalone-ic specific logs
            // (because jps rebuilds all files from last build if it failed and gradle rebuilds everything)
            allSourcesToCompile.addAll(sourcesToCompile)
            val text = allSourcesToCompile.joinToString(separator = System.getProperty("line.separator")) { it.canonicalPath }
            dirtySourcesSinceLastTimeFile.writeText(text)

            val services = Services.Builder().apply {
                register(LookupTracker::class.java, lookupTracker)
                register(CompilationCanceledStatus::class.java, EmptyCompilationCanceledStatus)
            }

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
            val compilationResult = compareAndUpdateCache(caches, generatedFiles)

            if (compilationMode is CompilationMode.Rebuild) break

            val (dirtyLookupSymbols, dirtyClassFqNames) = compilationResult.getDirtyData(listOf(caches.platformCache), reporter)
            val compiledInThisIterationSet = sourcesToCompile.toHashSet()

            with (dirtySources) {
                clear()
                addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter, excludes = compiledInThisIterationSet))
                addAll(mapClassesFqNamesToFiles(listOf(caches.platformCache), dirtyClassFqNames, reporter, excludes = compiledInThisIterationSet))
            }

            buildDirtyLookupSymbols.addAll(dirtyLookupSymbols)
            buildDirtyFqNames.addAll(dirtyClassFqNames)
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
}

class IncrementalJvmCompilerRunner(
        workingDir: File,
        private val javaSourceRoots: Set<JvmSourceRoot>,
        cacheVersions: List<CacheVersion>,
        reporter: ICReporter,
        private var kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null,
        artifactChangesProvider: ArtifactChangesProvider? = null,
        changesRegistry: ChangesRegistry? = null
) : IncrementalCompilerRunner<K2JVMCompilerArguments, IncrementalJvmCachesManager>(
        workingDir,
        CACHES_DIR_NAME,
        cacheVersions,
        reporter,
        artifactChangesProvider,
        changesRegistry
) {
    override fun isICEnabled(): Boolean =
            IncrementalCompilation.isEnabled()

    override fun createCacheManager(args: K2JVMCompilerArguments): IncrementalJvmCachesManager =
            IncrementalJvmCachesManager(cacheDirectory, File(args.destination), reporter)

    override fun destionationDir(args: K2JVMCompilerArguments): File =
            args.destinationAsFile

    private var javaFilesProcessor = ChangedJavaFilesProcessor(reporter)

    override fun calculateSourcesToCompile(caches: IncrementalJvmCachesManager, changedFiles: ChangedFiles.Known, args: K2JVMCompilerArguments): CompilationMode {
        val removedClassFiles = changedFiles.removed.filter(File::isClassFile)
        if (removedClassFiles.any()) return CompilationMode.Rebuild { "Removed class files: ${reporter.pathsAsString(removedClassFiles)}" }

        val modifiedClassFiles = changedFiles.modified.filter(File::isClassFile)
        if (modifiedClassFiles.any()) return CompilationMode.Rebuild { "Modified class files: ${reporter.pathsAsString(modifiedClassFiles)}" }

        val classpathSet = args.classpathAsList.toHashSet()
        val modifiedClasspathEntries = changedFiles.modified.filter { it in classpathSet }
        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
        reporter.report { "Last Kotlin Build info -- $lastBuildInfo" }
        val classpathChanges = getClasspathChanges(modifiedClasspathEntries, lastBuildInfo)
        if (classpathChanges !is ChangesEither.Known) {
            return CompilationMode.Rebuild { "could not get changes from modified classpath entries: ${reporter.pathsAsString(modifiedClasspathEntries)}" }
        }

        val javaFilesChanges = javaFilesProcessor.process(changedFiles)
        val affectedJavaSymbols = when (javaFilesChanges) {
            is ChangesEither.Known -> javaFilesChanges.lookupSymbols
            is ChangesEither.Unknown -> return CompilationMode.Rebuild { "Could not get changes for java files" }
        }

        val dirtyFiles = getDirtyFiles(changedFiles)
        val lookupSymbols = HashSet<LookupSymbol>()
        lookupSymbols.addAll(affectedJavaSymbols)
        lookupSymbols.addAll(classpathChanges.lookupSymbols)

        if (lookupSymbols.any()) {
            val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, reporter)
            dirtyFiles.addAll(dirtyFilesFromLookups)
        }

        val dirtyClassesFqNames = classpathChanges.fqNames.flatMap { withSubtypes(it, listOf(caches.platformCache)) }
        if (dirtyClassesFqNames.any()) {
            val dirtyFilesFromFqNames = mapClassesFqNamesToFiles(listOf(caches.platformCache), dirtyClassesFqNames, reporter)
            dirtyFiles.addAll(dirtyFilesFromFqNames)
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

    override fun preBuildHook(args: K2JVMCompilerArguments, compilationMode: CompilationMode) {
        when (compilationMode) {
            is CompilationMode.Incremental -> {
                args.classpathAsList += args.destinationAsFile.apply { mkdirs() }
            }
            is CompilationMode.Rebuild -> {
                // there is no point in updating annotation file since all files will be compiled anyway
                kaptAnnotationsFileUpdater = null
            }
        }
    }

    private var outdatedClasses: Iterable<JvmClassName> = emptyList()
    override fun markOutputDirty(caches: IncrementalJvmCachesManager, dirtySources: List<File>) {
        outdatedClasses = caches.platformCache.classesBySources(dirtySources)
        caches.platformCache.markOutputClassesDirty(dirtySources)
        super.markOutputDirty(caches, dirtySources)
    }

    override fun postCompilationHook(exitCode: ExitCode) {
        if (exitCode == ExitCode.OK) {
            kaptAnnotationsFileUpdater?.updateAnnotations(outdatedClasses)
        }
        else {
            kaptAnnotationsFileUpdater?.revert()
        }
    }

    override fun compareAndUpdateCache(caches: IncrementalJvmCachesManager, generatedFiles: List<GeneratedFile>): CompilationResult =
        updateIncrementalCache(generatedFiles, caches.platformCache)

    override fun additionalDirtyFiles(
            caches: IncrementalJvmCachesManager,
            generatedFiles: List<GeneratedFile>
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

            when (outputClass.classHeader.kind) {
                KotlinClassHeader.Kind.CLASS -> {
                    val fqName = outputClass.className.fqNameForClassNameWithoutDollars
                    val cachedSourceFile = cache.getSourceFileIfClass(fqName)

                    if (cachedSourceFile != null) {
                        // todo: seems useless, remove?
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

    override fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
            javaFilesProcessor.allChangedSymbols

    override fun runCompiler(
            sourcesToCompile: Set<File>,
            args: K2JVMCompilerArguments,
            caches: IncrementalJvmCachesManager,
            services: Services.Builder,
            messageCollector: MessageCollector
    ): ExitCode {
        val compiler = K2JVMCompiler()
        val outputDir = args.destinationAsFile
        val classpath = args.classpathAsList
        val moduleFile = makeModuleFile(args.moduleName!!,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = javaSourceRoots,
                classpath = classpath,
                friendDirs = listOf())
        val destination = args.destination
        args.destination = null
        args.buildFile = moduleFile.absolutePath

        try {
            val targetId = TargetId(args.buildFile!!, "java-production")
            val targetToCache = mapOf(targetId to caches.platformCache)
            val incrementalComponents = IncrementalCompilationComponentsImpl(targetToCache)
            services.register(IncrementalCompilationComponents::class.java, incrementalComponents)

            reporter.report { "compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}" }
            reporter.report { "compiling with classpath: ${classpath.toList().sorted().joinToString()}" }
            val exitCode = compiler.exec(messageCollector, services.build(), args)
            reporter.reportCompileIteration(sourcesToCompile, exitCode)
            return exitCode
        }
        finally {
            args.destination = destination
            moduleFile.delete()
        }
    }

    companion object {
        const val CACHES_DIR_NAME = "caches"
    }
}

private object EmptyCompilationCanceledStatus : CompilationCanceledStatus {
    override fun checkCanceled() {
    }
}

var K2JVMCompilerArguments.destinationAsFile: File
        get() = File(destination)
        set(value) { destination = value.path }

var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath!!.split(File.pathSeparator).map(::File)
    set(value) { classpath = value.joinToString(separator = File.pathSeparator, transform = { it.path }) }
