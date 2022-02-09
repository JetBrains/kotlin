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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotDisabled
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.ClasspathChanges.NotAvailableForJSCompiler
import org.jetbrains.kotlin.incremental.classpathDiff.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.util.BufferingMessageCollector
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File

@TestOnly
fun makeIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JVMCompilerArguments,
    messageCollector: MessageCollector = MessageCollector.NONE,
    reporter: ICReporter = EmptyICReporter
) {
    val kotlinExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
    val allExtensions = kotlinExtensions + "java"
    val rootsWalk = sourceRoots.asSequence().flatMap { it.walk() }
    val files = rootsWalk.filter(File::isFile)
    val sourceFiles = files.filter { it.extension.lowercase() in allExtensions }.toList()
    val buildHistoryFile = File(cachesDir, "build-history.bin")
    args.javaSourceRoots = sourceRoots.map { it.absolutePath }.toTypedArray()
    val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)

    withIC(args) {
        val compiler = IncrementalJvmCompilerRunner(
            cachesDir,
            buildReporter,
            // Use precise setting in case of non-Gradle build
            usePreciseJavaTracking = !args.useFir, // TODO: add fir-based java classes tracker when available and set this to true
            outputFiles = emptyList(),
            buildHistoryFile = buildHistoryFile,
            modulesApiHistory = EmptyModulesApiHistory,
            kotlinSourceFilesExtensions = kotlinExtensions,
            classpathChanges = ClasspathSnapshotDisabled
        )
        //TODO set properly
        compiler.compile(sourceFiles, args, messageCollector, providedChangedFiles = null)
    }
}

object EmptyICReporter : ICReporterBase() {
    override fun report(message: () -> String) {}
    override fun reportVerbose(message: () -> String) {}
    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {}
    override fun reportMarkDirtyClass(affectedFiles: Iterable<File>, classFqName: String) {}
    override fun reportMarkDirtyMember(affectedFiles: Iterable<File>, scope: String, name: String) {}
    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {}
}

@Suppress("DEPRECATION")
inline fun <R> withIC(args: CommonCompilerArguments, enabled: Boolean = true, fn: () -> R): R {
    val isEnabledBackup = IncrementalCompilation.isEnabledForJvm()
    IncrementalCompilation.setIsEnabledForJvm(enabled)

    try {
        if (args.incrementalCompilation == null) {
            args.incrementalCompilation = enabled
        }
        return fn()
    } finally {
        IncrementalCompilation.setIsEnabledForJvm(isEnabledBackup)
    }
}

class IncrementalJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter,
    private val usePreciseJavaTracking: Boolean,
    buildHistoryFile: File,
    outputFiles: Collection<File>,
    private val modulesApiHistory: ModulesApiHistory,
    override val kotlinSourceFilesExtensions: List<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    private val classpathChanges: ClasspathChanges
) : IncrementalCompilerRunner<K2JVMCompilerArguments, IncrementalJvmCachesManager>(
    workingDir,
    "caches-jvm",
    reporter,
    additionalOutputFiles = outputFiles,
    buildHistoryFile = buildHistoryFile
) {
    override fun createCacheManager(args: K2JVMCompilerArguments, projectDir: File?): IncrementalJvmCachesManager =
        IncrementalJvmCachesManager(
            cacheDirectory,
            projectDir,
            File(args.destination),
            reporter,
            storeFullFqNamesInLookupCache = withSnapshot || classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled,
            trackChangesInLookupCache = classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun
        )

    override fun destinationDir(args: K2JVMCompilerArguments): File =
        args.destinationAsFile

    private var dirtyClasspathChanges: Collection<FqName> = emptySet()

    private val psiFileProvider = object {
        val messageCollector = BufferingMessageCollector()

        fun javaFile(file: File): PsiFile? =
            psiFileFactory.createFileFromText(file.nameWithoutExtension, JavaLanguage.INSTANCE, file.readText())

        private val psiFileFactory: PsiFileFactory by lazy {
            val rootDisposable = Disposer.newDisposable()
            val configuration = CompilerConfiguration()
            val filterMessageCollector = FilteringMessageCollector(messageCollector, { !it.isError })
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, filterMessageCollector)
            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val project = environment.project
            PsiFileFactory.getInstance(project)
        }
    }

    private val changedUntrackedJavaClasses = mutableSetOf<ClassId>()

    private var javaFilesProcessor =
        if (!usePreciseJavaTracking)
            ChangedJavaFilesProcessor(reporter) { psiFileProvider.javaFile(it) }
        else
            null

    override fun calculateSourcesToCompileImpl(
        caches: IncrementalJvmCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode {
        return try {
            calculateSourcesToCompileImpl(caches, changedFiles, args, classpathAbiSnapshots)
        } finally {
            psiFileProvider.messageCollector.flush(messageCollector)
            psiFileProvider.messageCollector.clear()
        }
    }

    //TODO can't use the same way as for build-history files because abi-snapshot for all dependencies should be stored into last-build
    // and not only changed one
    // (but possibly we dont need to read it all and may be it is possible to update only those who was changed)
    override fun setupJarDependencies(args: K2JVMCompilerArguments, withSnapshot: Boolean, reporter: BuildReporter): Map<String, AbiSnapshot> {
        //fill abiSnapshots
        if (!withSnapshot) return emptyMap()
        val abiSnapshots = HashMap<String, AbiSnapshot>()
        args.classpathAsList
            .filter { it.extension.equals("jar", ignoreCase = true) }
            .forEach {
                modulesApiHistory.abiSnapshot(it).let { result ->
                    if (result is Either.Success<Set<File>>) {
                        result.value.forEach { file ->
                            AbiSnapshotImpl.read(file, reporter)?.also { abiSnapshot -> abiSnapshots[it.absolutePath] = abiSnapshot }
                        }
                    }
                }
            }
        return abiSnapshots
    }

    // Used by `calculateSourcesToCompileImpl` and `performWorkAfterSuccessfulCompilation` methods below.
    // Thread safety: There is no concurrent access to these variables.
    private var currentClasspathSnapshot: List<ClassSnapshotWithHash>? = null
    private var shrunkCurrentClasspathAgainstPreviousLookups: List<ClassSnapshotWithHash>? = null

    private fun calculateSourcesToCompileImpl(
        caches: IncrementalJvmCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JVMCompilerArguments,
        abiSnapshots: Map<String, AbiSnapshot> = HashMap()
    ): CompilationMode {
        val dirtyFiles = DirtyFilesContainer(caches, reporter, kotlinSourceFilesExtensions)
        initDirtyFiles(dirtyFiles, changedFiles)

        val classpathChanges = when (classpathChanges) {
            // Note: classpathChanges is deserialized, so they are no longer singleton objects and need to be compared using `is` (not `==`)
            is NoChanges -> ChangesEither.Known(emptySet(), emptySet())
            is ToBeComputedByIncrementalCompiler -> reporter.measure(BuildTime.COMPUTE_CLASSPATH_CHANGES) {
                check(currentClasspathSnapshot == null)
                currentClasspathSnapshot = reporter.measure(BuildTime.LOAD_CURRENT_CLASSPATH_SNAPSHOT) {
                    val classpathSnapshot =
                        CachedClasspathSnapshotSerializer.load(classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles)
                    reporter.measure(BuildTime.REMOVE_DUPLICATE_CLASSES) {
                        classpathSnapshot.removeDuplicateAndInaccessibleClasses()
                    }
                }
                check(shrunkCurrentClasspathAgainstPreviousLookups == null)
                shrunkCurrentClasspathAgainstPreviousLookups = reporter.measure(BuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT) {
                    ClasspathSnapshotShrinker.shrink(currentClasspathSnapshot!!, caches.lookupCache, reporter)
                }
                ClasspathChangesComputer.computeChangedAndImpactedSet(
                    shrunkCurrentClasspathAgainstPreviousLookups!!,
                    classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile,
                    reporter
                ).getChanges()
            }
            is NotAvailableDueToMissingClasspathSnapshot -> ChangesEither.Unknown(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND)
            is NotAvailableForNonIncrementalRun -> ChangesEither.Unknown(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ClasspathSnapshotDisabled -> reporter.measure(BuildTime.IC_ANALYZE_CHANGES_IN_DEPENDENCIES) {
                if (!withSnapshot && !buildHistoryFile.isFile) {
                    // If the previous build was a Gradle cache hit, the build history file must have been deleted as it is marked as
                    // @LocalState in the Gradle task. Therefore, this compilation will need to run non-incrementally.
                    // (Note that buildHistoryFile is outside workingDir. We don't need to perform the same check for files inside
                    // workingDir as workingDir is an @OutputDirectory, so the files must be present in an incremental build.)
                    return CompilationMode.Rebuild(BuildAttribute.NO_BUILD_HISTORY)
                }
                val lastBuildInfo = BuildInfo.read(lastBuildInfoFile)
                reporter.reportVerbose { "Last Kotlin Build info -- $lastBuildInfo" }
                val scopes = caches.lookupCache.lookupSymbols.map { it.scope.ifBlank { it.name } }.distinct()

                getClasspathChanges(
                    args.classpathAsList, changedFiles, lastBuildInfo, modulesApiHistory, reporter, abiSnapshots, withSnapshot,
                    caches.platformCache, scopes
                )
            }
            is NotAvailableForJSCompiler -> error("Unexpected type for this code path: ${classpathChanges.javaClass.name}.")
        }

        @Suppress("UNUSED_VARIABLE") // for sealed when
        val unused = when (classpathChanges) {
            is ChangesEither.Unknown -> {
                reporter.report {
                    "Could not get classpath's changes: ${classpathChanges.reason}"
                }
                return CompilationMode.Rebuild(classpathChanges.reason)
            }
            is ChangesEither.Known -> {
                dirtyFiles.addByDirtySymbols(classpathChanges.lookupSymbols)
                dirtyClasspathChanges = classpathChanges.fqNames
                dirtyFiles.addByDirtyClasses(classpathChanges.fqNames)
            }
        }

        reporter.measure(BuildTime.IC_ANALYZE_CHANGES_IN_JAVA_SOURCES) {
            if (!usePreciseJavaTracking) {
                val javaFilesChanges = javaFilesProcessor!!.process(changedFiles)
                val affectedJavaSymbols = when (javaFilesChanges) {
                    is ChangesEither.Known -> javaFilesChanges.lookupSymbols
                    is ChangesEither.Unknown -> return CompilationMode.Rebuild(javaFilesChanges.reason)
                }
                dirtyFiles.addByDirtySymbols(affectedJavaSymbols)
            } else {
                val rebuildReason = processChangedJava(changedFiles, caches)
                if (rebuildReason != null) return CompilationMode.Rebuild(rebuildReason)
            }
        }

        val androidLayoutChanges = reporter.measure(BuildTime.IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS) {
            processLookupSymbolsForAndroidLayouts(changedFiles)
        }
        val removedClassesChanges = reporter.measure(BuildTime.IC_DETECT_REMOVED_CLASSES) {
            getRemovedClassesChanges(caches, changedFiles)
        }

        dirtyFiles.addByDirtySymbols(androidLayoutChanges)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNamesForceRecompile)
        return CompilationMode.Incremental(dirtyFiles)
    }

    private fun processChangedJava(changedFiles: ChangedFiles.Known, caches: IncrementalJvmCachesManager): BuildAttribute? {
        val javaFiles = (changedFiles.modified + changedFiles.removed).filter(File::isJavaFile)

        for (javaFile in javaFiles) {
            if (!caches.platformCache.isTrackedFile(javaFile)) {
                if (!javaFile.exists()) {
                    // todo: can we do this more optimal?
                    reporter.report { "Could not get changed for untracked removed java file $javaFile" }
                    return BuildAttribute.JAVA_CHANGE_UNTRACKED_FILE_IS_REMOVED
                }

                val psiFile = psiFileProvider.javaFile(javaFile)
                if (psiFile !is PsiJavaFile) {
                    reporter.report { "[Precise Java tracking] Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                    return BuildAttribute.JAVA_CHANGE_UNEXPECTED_PSI
                }

                for (psiClass in psiFile.classes) {
                    val qualifiedName = psiClass.qualifiedName
                    if (qualifiedName == null) {
                        reporter.report { "[Precise Java tracking] Class with unknown qualified name in $javaFile" }
                        return BuildAttribute.JAVA_CHANGE_UNKNOWN_QUALIFIER
                    }

                    processChangedUntrackedJavaClass(psiClass, ClassId.topLevel(FqName(qualifiedName)))
                }
            }
        }

        caches.platformCache.markDirty(javaFiles)
        return null
    }

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
            if (file.extension.lowercase() != "xml") continue
            val layoutName = file.name.substringBeforeLast('.')
            result.add(LookupSymbol(ANDROID_LAYOUT_CONTENT_LOOKUP_NAME, layoutName))
        }

        return result
    }

    override fun preBuildHook(args: K2JVMCompilerArguments, compilationMode: CompilationMode) {
        if (compilationMode is CompilationMode.Incremental) {
            val destinationDir = args.destinationAsFile
            destinationDir.mkdirs()
            args.classpathAsList = listOf(destinationDir) + args.classpathAsList
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
        caches.platformCache.getObsoleteJavaClasses().isNotEmpty() || changedUntrackedJavaClasses.isNotEmpty()

    override fun additionalDirtyFiles(
        caches: IncrementalJvmCachesManager,
        generatedFiles: List<GeneratedFile>,
        services: Services
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
                KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.SYNTHETIC_CLASS, KotlinClassHeader.Kind.UNKNOWN -> {
                }
            }
        }

        return result
    }

    override fun additionalDirtyLookupSymbols(): Iterable<LookupSymbol> =
        javaFilesProcessor?.allChangedSymbols ?: emptyList()

    override fun makeServices(
        args: K2JVMCompilerArguments,
        lookupTracker: LookupTracker,
        expectActualTracker: ExpectActualTracker,
        caches: IncrementalJvmCachesManager,
        dirtySources: Set<File>,
        isIncremental: Boolean
    ): Services.Builder =
        super.makeServices(args, lookupTracker, expectActualTracker, caches, dirtySources, isIncremental).apply {
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
        val freeArgsBackup = args.freeArgs.toList()
        args.freeArgs += sourcesToCompile.map { it.absolutePath }
        args.allowNoSourceFiles = true
        val exitCode = compiler.exec(messageCollector, services, args)
        args.freeArgs = freeArgsBackup
        reportPerformanceData(compiler.defaultPerformanceManager)
        return exitCode
    }

    override fun performWorkAfterSuccessfulCompilation(caches: IncrementalJvmCachesManager) {
        if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
            reporter.measure(BuildTime.SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                shrinkAndSaveClasspathSnapshot(
                    classpathChanges, caches.lookupCache, currentClasspathSnapshot, shrunkCurrentClasspathAgainstPreviousLookups,
                    reporter
                )
            }
        }
    }
}

var K2JVMCompilerArguments.destinationAsFile: File
    get() = File(destination)
    set(value) {
        destination = value.path
    }

var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath.orEmpty().split(File.pathSeparator).map(::File)
    set(value) {
        classpath = value.joinToString(separator = File.pathSeparator, transform = { it.path })
    }
