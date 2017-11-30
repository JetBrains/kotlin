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

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
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
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import java.io.File

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
    override fun report(message: () -> String) {
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
        changesRegistry: ChangesRegistry? = null,
        private val buildHistoryFile: File? = null,
        private val friendBuildHistoryFile: File? = null
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

    override fun calculateSourcesToCompile(caches: IncrementalJvmCachesManager, changedFiles: ChangedFiles.Known, args: K2JVMCompilerArguments): CompilationMode {
        val dirtyFiles = getDirtyFiles(changedFiles)

        fun markDirtyBy(lookupSymbols: Collection<LookupSymbol>) {
            if (lookupSymbols.isEmpty()) return

            val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, reporter)
            dirtyFiles.addAll(dirtyFilesFromLookups)
        }

        fun markDirtyBy(dirtyClassesFqNames: Collection<FqName>) {
            if (dirtyClassesFqNames.isEmpty()) return

            val fqNamesWithSubtypes = dirtyClassesFqNames.flatMap { withSubtypes(it, listOf(caches.platformCache)) }
            val dirtyFilesFromFqNames = mapClassesFqNamesToFiles(listOf(caches.platformCache), fqNamesWithSubtypes, reporter)
            dirtyFiles.addAll(dirtyFilesFromFqNames)
        }

        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
        reporter.report { "Last Kotlin Build info -- $lastBuildInfo" }

        val changesFromFriend by lazy {
            val myLastTS = lastBuildInfo?.startTS ?: return@lazy ChangesEither.Unknown()
            val storage = friendBuildHistoryFile?.let { BuildDiffsStorage.readFromFile(it, reporter) } ?: return@lazy ChangesEither.Unknown()

            val (prevDiffs, newDiffs) = storage.buildDiffs.partition { it.ts < myLastTS }
            if (prevDiffs.isEmpty()) return@lazy ChangesEither.Unknown()

            val dirtyLookupSymbols = HashSet<LookupSymbol>()
            val dirtyClassesFqNames = HashSet<FqName>()
            for ((_, isIncremental, dirtyData) in newDiffs) {
                if (!isIncremental) return@lazy ChangesEither.Unknown()

                dirtyLookupSymbols.addAll(dirtyData.dirtyLookupSymbols)
                dirtyClassesFqNames.addAll(dirtyData.dirtyClassesFqNames)
            }

            markDirtyBy(dirtyLookupSymbols)
            markDirtyBy(dirtyClassesFqNames)
            ChangesEither.Known(dirtyLookupSymbols, dirtyClassesFqNames)
        }
        val friendDirs = args.friendPaths?.map { File(it) } ?: emptyList()
        for (file in changedFiles.removed.asSequence() + changedFiles.modified.asSequence()) {
            if (!file.isClassFile()) continue

            val isFriendClassFile = friendDirs.any { FileUtil.isAncestor(it, file, false) }
            if (isFriendClassFile && changesFromFriend is ChangesEither.Known) continue

            return CompilationMode.Rebuild { "Cannot get changes from modified or removed class file: ${reporter.pathsAsString(file)}" }
        }

        val classpathSet = args.classpathAsList.toHashSet()
        val modifiedClasspathEntries = changedFiles.modified.filter { it in classpathSet }
        val classpathChanges = getClasspathChanges(modifiedClasspathEntries, lastBuildInfo)
        if (classpathChanges !is ChangesEither.Known) {
            return CompilationMode.Rebuild { "could not get changes from modified classpath entries: ${reporter.pathsAsString(modifiedClasspathEntries)}" }
        }

        processChangedJava(changedFiles, caches)

        if ((changedFiles.modified + changedFiles.removed).any { it.extension.toLowerCase() == "xml" }) {
            return CompilationMode.Rebuild { "XML resource files were changed" }
        }

        markDirtyBy(classpathChanges.lookupSymbols)
        markDirtyBy(classpathChanges.fqNames)

        return CompilationMode.Incremental(dirtyFiles)
    }

    private fun processChangedJava(changedFiles: ChangedFiles.Known, caches: IncrementalJvmCachesManager) {
        caches.platformCache.markDirty((changedFiles.modified + changedFiles.removed).filter(File::isJavaFile))
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

    override fun postCompilationHook(exitCode: ExitCode) {
        if (exitCode == ExitCode.OK) {
            // TODO: Is it ok that argument always was an empty list?
            kaptAnnotationsFileUpdater?.updateAnnotations(emptyList())
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
        updateIncrementalCache(
                generatedFiles, caches.platformCache, changesCollector,
                services[JavaClassesTracker::class.java] as? JavaClassesTrackerImpl
        )
    }

    override fun runWithNoDirtyKotlinSources(caches: IncrementalJvmCachesManager): Boolean =
            caches.platformCache.getObsoleteJavaClasses().isNotEmpty()

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

    override fun processChangesAfterBuild(compilationMode: CompilationMode, currentBuildInfo: BuildInfo, dirtyData: DirtyData) {
        super.processChangesAfterBuild(compilationMode, currentBuildInfo, dirtyData)

        if (buildHistoryFile == null) return

        val prevDiffs = BuildDiffsStorage.readFromFile(buildHistoryFile, reporter)?.buildDiffs ?: emptyList()
        val newDiff = if (compilationMode is CompilationMode.Incremental) {
            BuildDifference(currentBuildInfo.startTS, true, dirtyData)
        }
        else {
            val emptyDirtyData = DirtyData()
            BuildDifference(currentBuildInfo.startTS, false, emptyDirtyData)
        }

        BuildDiffsStorage.writeToFile(buildHistoryFile, BuildDiffsStorage(prevDiffs + newDiff), reporter)
    }

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
            val changesTracker = JavaClassesTrackerImpl(caches.platformCache)
            register(JavaClassesTracker::class.java, changesTracker)
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
