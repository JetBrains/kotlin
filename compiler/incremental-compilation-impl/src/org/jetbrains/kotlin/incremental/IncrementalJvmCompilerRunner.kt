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

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
    val buildHistoryFile = File(cachesDir, "build-history.bin")

    withIC {
        val compiler = IncrementalJvmCompilerRunner(
                cachesDir,
                sourceRoots.map { JvmSourceRoot(it, null) }.toSet(),
                versions, reporter,
                // Use precise setting in case of non-Gradle build
                usePreciseJavaTracking = true,
                localStateDirs = emptyList(),
                buildHistoryFile = buildHistoryFile,
                modulesApiHistory = EmptyModulesApiHistory
        )
        compiler.compile(sourceFiles, args, messageCollector, providedChangedFiles = null)
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
        private val usePreciseJavaTracking: Boolean,
        private val buildHistoryFile: File,
        localStateDirs: Collection<File>,
        private val modulesApiHistory: ModulesApiHistory
) : IncrementalCompilerRunner<K2JVMCompilerArguments, IncrementalJvmCachesManager>(
        workingDir,
        "caches-jvm",
        cacheVersions,
        reporter,
        localStateDirs = localStateDirs
) {
    override fun isICEnabled(): Boolean =
            IncrementalCompilation.isEnabled()

    override fun createCacheManager(args: K2JVMCompilerArguments): IncrementalJvmCachesManager =
            IncrementalJvmCachesManager(cacheDirectory, File(args.destination), reporter)

    override fun destinationDir(args: K2JVMCompilerArguments): File =
            args.destinationAsFile

    private val psiFileFactory: PsiFileFactory by lazy {
        val rootDisposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project
        PsiFileFactory.getInstance(project)
    }

    private val changedUntrackedJavaClasses = mutableSetOf<ClassId>()

    private var javaFilesProcessor =
            if (!usePreciseJavaTracking)
                ChangedJavaFilesProcessor(reporter) { it.psiFile() }
            else
                null

    override fun calculateSourcesToCompile(
        caches: IncrementalJvmCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JVMCompilerArguments
    ): CompilationMode {
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

        val lastBuildInfo = BuildInfo.read(lastBuildInfoFile) ?: return CompilationMode.Rebuild { "No information on previous build" }
        reporter.report { "Last Kotlin Build info -- $lastBuildInfo" }

        val classpathChanges = getClasspathChanges(args.classpathAsList, changedFiles, lastBuildInfo)

        @Suppress("UNUSED_VARIABLE") // for sealed when
        val unused = when (classpathChanges) {
            is ChangesEither.Unknown -> return CompilationMode.Rebuild {
                // todo: we can recompile all files incrementally (not cleaning caches), so rebuild won't propagate
                "Could not get classpath's changes${classpathChanges.reason?.let { ": $it" }}"
            }
            is ChangesEither.Known -> {
                markDirtyBy(classpathChanges.lookupSymbols)
                markDirtyBy(classpathChanges.fqNames)
            }
        }

        if (!usePreciseJavaTracking) {
            val javaFilesChanges = javaFilesProcessor!!.process(changedFiles)
            val affectedJavaSymbols = when (javaFilesChanges) {
                is ChangesEither.Known -> javaFilesChanges.lookupSymbols
                is ChangesEither.Unknown -> return CompilationMode.Rebuild { "Could not get changes for java files" }
            }
            markDirtyBy(affectedJavaSymbols)
        }
        else {
            if (!processChangedJava(changedFiles, caches)) {
                return CompilationMode.Rebuild { "Could not get changes for java files" }
            }
        }

        val androidLayoutChanges = processLookupSymbolsForAndroidLayouts(changedFiles)
        val removedClassesChanges = getRemovedClassesChanges(caches, changedFiles)

        markDirtyBy(androidLayoutChanges)
        markDirtyBy(removedClassesChanges.dirtyLookupSymbols)
        markDirtyBy(removedClassesChanges.dirtyClassesFqNames)

        return CompilationMode.Incremental(dirtyFiles)
    }

    private fun processChangedJava(changedFiles: ChangedFiles.Known, caches: IncrementalJvmCachesManager): Boolean {
        val javaFiles = (changedFiles.modified + changedFiles.removed).filter(File::isJavaFile)

        for (javaFile in javaFiles) {
            if (!caches.platformCache.isTrackedFile(javaFile)) {
                if (!javaFile.exists()) {
                    // todo: can we do this more optimal?
                    reporter.report { "Could not get changed for untracked removed java file $javaFile" }
                    return false
                }

                val psiFile = javaFile.psiFile()
                if (psiFile !is PsiJavaFile) {
                    reporter.report { "[Precise Java tracking] Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                    return false
                }

                for (psiClass in psiFile.classes) {
                    val qualifiedName = psiClass.qualifiedName
                    if (qualifiedName == null) {
                        reporter.report { "[Precise Java tracking] Class with unknown qualified name in $javaFile" }
                        return false
                    }

                    processChangedUntrackedJavaClass(psiClass, ClassId.topLevel(FqName(qualifiedName)))
                }
            }
        }

        caches.platformCache.markDirty(javaFiles)
        return true
    }

    private fun File.psiFile(): PsiFile? =
            psiFileFactory.createFileFromText(nameWithoutExtension, JavaLanguage.INSTANCE, readText())

    private fun processChangedUntrackedJavaClass(psiClass: PsiClass, classId: ClassId) {
        changedUntrackedJavaClasses.add(classId)
        for (innerClass in psiClass.innerClasses) {
            val name = innerClass.name ?: continue
            processChangedUntrackedJavaClass(innerClass, classId.createNestedClassId(Name.identifier(name)))
        }
    }

    private fun processLookupSymbolsForAndroidLayouts(changedFiles: ChangedFiles.Known): Collection<LookupSymbol> {
        val result = mutableListOf<LookupSymbol>()
        for (file in changedFiles.modified + changedFiles.removed) {
            if (file.extension.toLowerCase() != "xml") continue
            val layoutName = file.name.substringBeforeLast('.')
            result.add(LookupSymbol(ANDROID_LAYOUT_CONTENT_LOOKUP_NAME, layoutName))
        }

        return result
    }

    private fun getClasspathChanges(
        classpath: List<File>,
        changedFiles: ChangedFiles.Known,
        lastBuildInfo: BuildInfo
    ): ChangesEither {
        val classpathSet = HashSet<File>()
        for (file in classpath) {
            when {
                file.isFile -> classpathSet.add(file)
                file.isDirectory -> file.walk().filterTo(classpathSet) { it.isFile }
            }
        }

        val modifiedClasspath = changedFiles.modified.filterTo(HashSet()) { it in classpathSet }
        val removedClasspath = changedFiles.removed.filterTo(HashSet()) { it in classpathSet }

        // todo: removed classes could be processed normally
        if (removedClasspath.isNotEmpty()) return ChangesEither.Unknown("Some files are removed from classpath $removedClasspath")

        if (modifiedClasspath.isEmpty()) return ChangesEither.Known()

        val lastBuildTS = lastBuildInfo.startTS

        val symbols = HashSet<LookupSymbol>()
        val fqNames = HashSet<FqName>()

        val historyFilesEither = modulesApiHistory.historyFilesForChangedFiles(modifiedClasspath)
        val historyFiles = when (historyFilesEither) {
            is Either.Success<Set<File>> -> historyFilesEither.value
            is Either.Error -> return ChangesEither.Unknown(historyFilesEither.reason)
        }

        for (historyFile in historyFiles) {
            val allBuilds = BuildDiffsStorage.readDiffsFromFile(historyFile, reporter = reporter)
                    ?: return ChangesEither.Unknown("Could not read diffs from $historyFile")
            val (knownBuilds, newBuilds) = allBuilds.partition { it.ts <= lastBuildTS }
            if (knownBuilds.isEmpty()) {
                return ChangesEither.Unknown("No previously known builds for $historyFile")
            }

            for (buildDiff in newBuilds) {
                if (!buildDiff.isIncremental) return ChangesEither.Unknown("Non-incremental build from dependency $historyFile")

                val dirtyData = buildDiff.dirtyData
                symbols.addAll(dirtyData.dirtyLookupSymbols)
                fqNames.addAll(dirtyData.dirtyClassesFqNames)
            }
        }

        return ChangesEither.Known(symbols, fqNames)
    }

    override fun preBuildHook(args: K2JVMCompilerArguments, compilationMode: CompilationMode) {
        if (compilationMode is CompilationMode.Incremental) {
            val destinationDir = args.destinationAsFile
            destinationDir.mkdirs()
            args.classpathAsList = listOf(destinationDir) + args.classpathAsList
        }
    }

    override fun postCompilationHook(exitCode: ExitCode) {}

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
            caches.platformCache.getObsoleteJavaClasses().isNotEmpty() || changedUntrackedJavaClasses.isNotEmpty()

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
            javaFilesProcessor?.allChangedSymbols ?: emptyList()

    override fun processChangesAfterBuild(
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

    override fun makeServices(
            args: K2JVMCompilerArguments,
            lookupTracker: LookupTracker,
            expectActualTracker: ExpectActualTracker,
            caches: IncrementalJvmCachesManager,
            compilationMode: CompilationMode
    ): Services.Builder =
        super.makeServices(args, lookupTracker, expectActualTracker, caches, compilationMode).apply {
            val targetId = TargetId(args.moduleName!!, "java-production")
            val targetToCache = mapOf(targetId to caches.platformCache)
            val incrementalComponents = IncrementalCompilationComponentsImpl(targetToCache)
            register(IncrementalCompilationComponents::class.java, incrementalComponents)
            if (usePreciseJavaTracking) {
                val changesTracker = JavaClassesTrackerImpl(caches.platformCache, changedUntrackedJavaClasses.toSet())
                changedUntrackedJavaClasses.clear()
                register(JavaClassesTracker::class.java, changesTracker)
            }
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
