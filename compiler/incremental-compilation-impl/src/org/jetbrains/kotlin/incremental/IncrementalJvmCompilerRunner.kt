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
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import org.jetbrains.kotlin.incremental.multiproject.ChangesRegistry
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
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

object EmptyICReporter : ICReporter {
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
        "caches-jvm",
        cacheVersions,
        reporter,
        artifactChangesProvider,
        changesRegistry
) {
    override fun isICEnabled(): Boolean =
            IncrementalCompilation.isEnabled()

    override fun createCacheManager(args: K2JVMCompilerArguments): IncrementalJvmCachesManager =
            IncrementalJvmCachesManager(cacheDirectory, File(args.destination), reporter)

    override fun destinationDir(args: K2JVMCompilerArguments): File =
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
    override fun markDirty(caches: IncrementalJvmCachesManager, dirtySources: List<File>) {
        outdatedClasses = caches.platformCache.classesBySources(dirtySources)
        super.markDirty(caches, dirtySources)
    }

    override fun postCompilationHook(exitCode: ExitCode) {
        if (exitCode == ExitCode.OK) {
            kaptAnnotationsFileUpdater?.updateAnnotations(outdatedClasses)
        }
        else {
            kaptAnnotationsFileUpdater?.revert()
        }
    }

    override fun updateCaches(
            services: Services,
            caches: IncrementalJvmCachesManager,
            generatedFiles: List<GeneratedFile>,
            changesCollector: ChangesCollector
    ) {
        updateIncrementalCache(generatedFiles, caches.platformCache, changesCollector)
    }

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

    override fun makeServices(
            args: K2JVMCompilerArguments,
            lookupTracker: LookupTracker,
            caches: IncrementalJvmCachesManager,
            compilationMode: CompilationMode
    ): Services.Builder =
        super.makeServices(args, lookupTracker, caches, compilationMode).apply {
            val targetId = TargetId(args.moduleName!!, "java-production")
            val targetToCache = mapOf(targetId to caches.platformCache)
            val incrementalComponents = IncrementalCompilationComponentsImpl(targetToCache)
            register(IncrementalCompilationComponents::class.java, incrementalComponents)
        }

    override fun runCompiler(
            sourcesToCompile: Set<File>,
            args: K2JVMCompilerArguments,
            caches: IncrementalJvmCachesManager,
            services: Services,
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
            reporter.report { "compiling with args: ${ArgumentUtils.convertArgumentsToStringList(args)}" }
            reporter.report { "compiling with classpath: ${classpath.toList().sorted().joinToString()}" }
            val exitCode = compiler.exec(messageCollector, services, args)
            reporter.reportCompileIteration(sourcesToCompile, exitCode)
            return exitCode
        }
        finally {
            args.destination = destination
            moduleFile.delete()
        }
    }
}

var K2JVMCompilerArguments.destinationAsFile: File
        get() = File(destination)
        set(value) { destination = value.path }

var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath!!.split(File.pathSeparator).map(::File)
    set(value) { classpath = value.joinToString(separator = File.pathSeparator, transform = { it.path }) }
