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
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.report.*
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotDisabled
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.ClasspathChanges.NotAvailableForJSCompiler
import org.jetbrains.kotlin.incremental.classpathDiff.*
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClasspathChanges
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
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

open class IncrementalJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    private val usePreciseJavaTracking: Boolean,
    buildHistoryFile: File?,
    outputDirs: Collection<File>?,
    private val modulesApiHistory: ModulesApiHistory,
    override val kotlinSourceFilesExtensions: List<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    private val classpathChanges: ClasspathChanges,
    withAbiSnapshot: Boolean = false,
    preciseCompilationResultsBackup: Boolean = false,
    keepIncrementalCompilationCachesInMemory: Boolean = false,
) : IncrementalCompilerRunner<K2JVMCompilerArguments, IncrementalJvmCachesManager>(
    workingDir,
    "caches-jvm",
    reporter,
    buildHistoryFile = buildHistoryFile,
    outputDirs = outputDirs,
    withAbiSnapshot = withAbiSnapshot,
    preciseCompilationResultsBackup = preciseCompilationResultsBackup,
    keepIncrementalCompilationCachesInMemory = keepIncrementalCompilationCachesInMemory,
) {
    override val shouldTrackChangesInLookupCache
        get() = classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun

    override val shouldStoreFullFqNamesInLookupCache
        get() = withAbiSnapshot || classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled

    override fun createCacheManager(icContext: IncrementalCompilationContext, args: K2JVMCompilerArguments) =
        IncrementalJvmCachesManager(icContext, args.destination?.let { File(it) }, cacheDirectory)

    override fun destinationDir(args: K2JVMCompilerArguments): File =
        args.destinationAsFile

    private val messageCollector = BufferingMessageCollector()
    private val compilerConfiguration: CompilerConfiguration by lazy {
        val filterMessageCollector = FilteringMessageCollector(messageCollector) { !it.isError }
        CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, filterMessageCollector)
            configureJdkClasspathRoots()
        }
    }

    private val psiFileProvider = object {
        fun javaFile(file: File): PsiFile? =
            psiFileFactory.createFileFromText(file.nameWithoutExtension, JavaLanguage.INSTANCE, file.readText())

        private val psiFileFactory: PsiFileFactory by lazy {
            val rootDisposable = Disposer.newDisposable()
            val configuration = compilerConfiguration
            val environment =
                KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
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

    override fun calculateSourcesToCompile(
        caches: IncrementalJvmCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode {
        return try {
            calculateSourcesToCompileImpl(caches, changedFiles, args, messageCollector, classpathAbiSnapshots, withAbiSnapshot)
        } finally {
            this.messageCollector.flush(messageCollector)
            this.messageCollector.clear()
        }
    }

    //TODO can't use the same way as for build-history files because abi-snapshot for all dependencies should be stored into last-build
    // and not only changed one
    // (but possibly we dont need to read it all and may be it is possible to update only those who was changed)
    override fun setupJarDependencies(args: K2JVMCompilerArguments, reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>): Map<String, AbiSnapshot> {
        //fill abiSnapshots
        val abiSnapshots = HashMap<String, AbiSnapshot>()
        args.classpathAsList
            .filter { it.extension.equals("jar", ignoreCase = true) }
            .forEach {
                modulesApiHistory.abiSnapshot(it).let { result ->
                    if (result is Either.Success<Set<File>>) {
                        result.value.forEach { file ->
                            if (file.exists()) {
                                abiSnapshots[it.absolutePath] = AbiSnapshotImpl.read(file)
                            } else {
                                // FIXME: We should throw an exception here
                                reporter.warn { "Snapshot file does not exist: ${file.path}. Continue anyway." }
                            }
                        }
                    }
                }
            }
        return abiSnapshots
    }

    // There are 2 steps where we need to load the current classpath snapshot and shrink it:
    //   - Before classpath diffing when `classpathChanges` is ToBeComputedByIncrementalCompiler (see `calculateSourcesToCompileImpl`)
    //   - After compilation (see `performWorkAfterSuccessfulCompilation`)
    // To avoid duplicated work, we store the snapshots after the first step for reuse (if the first step is executed).
    private var currentClasspathSnapshot: List<AccessibleClassSnapshot>? = null
    private var shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>? = null

    private fun calculateSourcesToCompileImpl(
        caches: IncrementalJvmCachesManager,
        changedFiles: ChangedFiles.Known,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector,
        abiSnapshots: Map<String, AbiSnapshot>,
        withAbiSnapshot: Boolean
    ): CompilationMode {
        val dirtyFiles = DirtyFilesContainer(caches, reporter, kotlinSourceFilesExtensions)
        initDirtyFiles(dirtyFiles, changedFiles)

        reporter.debug { "Classpath changes info passed from Gradle task: ${classpathChanges::class.simpleName}" }
        val changedAndImpactedSymbols = when (classpathChanges) {
            // Note: classpathChanges is deserialized, so they are no longer singleton objects and need to be compared using `is` (not `==`)
            is NoChanges -> ChangesEither.Known(emptySet(), emptySet())
            is ToBeComputedByIncrementalCompiler -> reporter.measure(GradleBuildTime.COMPUTE_CLASSPATH_CHANGES) {
                reporter.addMetric(GradleBuildPerformanceMetric.COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT, 1)
                val storeCurrentClasspathSnapshotForReuse =
                    { currentClasspathSnapshotArg: List<AccessibleClassSnapshot>,
                      shrunkCurrentClasspathAgainstPreviousLookupsArg: List<AccessibleClassSnapshot> ->
                        currentClasspathSnapshot = currentClasspathSnapshotArg
                        shrunkCurrentClasspathAgainstPreviousLookups = shrunkCurrentClasspathAgainstPreviousLookupsArg
                    }
                val classpathChanges = computeClasspathChanges(
                    classpathChanges.classpathSnapshotFiles,
                    caches.lookupCache,
                    storeCurrentClasspathSnapshotForReuse,
                    ClasspathSnapshotBuildReporter(reporter)
                )
                // `classpathChanges` contains changed and impacted symbols on the classpath.
                // We also need to compute symbols in the current module that are impacted by `classpathChanges`.
                classpathChanges.toChangeInfoList().getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter).toChangesEither()
            }
            is NotAvailableDueToMissingClasspathSnapshot -> ChangesEither.Unknown(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND)
            is NotAvailableForNonIncrementalRun -> ChangesEither.Unknown(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ClasspathSnapshotDisabled -> reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_DEPENDENCIES) {
                if (buildHistoryFile == null) {
                    error("The build is configured to use the build-history based IC approach, but doesn't specify the buildHistoryFile")
                }
                if (!withAbiSnapshot && buildHistoryFile.isFile != true) {
                    // If the previous build was a Gradle cache hit, the build history file must have been deleted as it is marked as
                    // @LocalState in the Gradle task. Therefore, this compilation will need to run non-incrementally.
                    // (Note that buildHistoryFile is outside workingDir. We don't need to perform the same check for files inside
                    // workingDir as workingDir is an @OutputDirectory, so the files must be present in an incremental build.)
                    return CompilationMode.Rebuild(BuildAttribute.NO_BUILD_HISTORY)
                }
                if (!lastBuildInfoFile.exists()) {
                    return CompilationMode.Rebuild(BuildAttribute.NO_LAST_BUILD_INFO)
                }
                val lastBuildInfo = BuildInfo.read(lastBuildInfoFile, messageCollector)
                    ?: return CompilationMode.Rebuild(BuildAttribute.INVALID_LAST_BUILD_INFO)
                reporter.debug { "Last Kotlin Build info -- $lastBuildInfo" }
                val scopes = caches.lookupCache.lookupSymbols.map { it.scope.ifBlank { it.name } }.distinct()

                // FIXME The old IC currently doesn't compute impacted symbols
                getClasspathChanges(
                    args.classpathAsList, changedFiles, lastBuildInfo, modulesApiHistory, reporter, abiSnapshots, withAbiSnapshot,
                    caches.platformCache, scopes
                )
            }
            is NotAvailableForJSCompiler -> error("Unexpected type for this code path: ${classpathChanges.javaClass.name}.")
        }

        when (changedAndImpactedSymbols) {
            is ChangesEither.Unknown -> {
                reporter.info { "Could not get classpath changes: ${changedAndImpactedSymbols.reason}" }
                return CompilationMode.Rebuild(changedAndImpactedSymbols.reason)
            }
            is ChangesEither.Known -> Unit
        }.forceExhaustiveWhen()

        dirtyFiles.addByDirtySymbols(changedAndImpactedSymbols.lookupSymbols)
        dirtyFiles.addByDirtyClasses(changedAndImpactedSymbols.fqNames)

        reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_JAVA_SOURCES) {
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

        val androidLayoutChanges = reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS) {
            processLookupSymbolsForAndroidLayouts(changedFiles)
        }
        val removedClassesChanges = reporter.measure(GradleBuildTime.IC_DETECT_REMOVED_CLASSES) {
            getRemovedClassesChanges(caches, changedFiles)
        }

        dirtyFiles.addByDirtySymbols(androidLayoutChanges)
        dirtyFiles.addByDirtySymbols(removedClassesChanges.dirtyLookupSymbols)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNames)
        dirtyFiles.addByDirtyClasses(removedClassesChanges.dirtyClassesFqNamesForceRecompile)
        return CompilationMode.Incremental(dirtyFiles)
    }

    private fun ProgramSymbolSet.toChangeInfoList(): List<ChangeInfo> {
        val changes = mutableListOf<ChangeInfo>()
        classes.forEach { classId ->
            // It's important to set `areSubclassesAffected = true` when we don't know
            changes.add(ChangeInfo.SignatureChanged(classId.asSingleFqName(), areSubclassesAffected = true))
        }
        classMembers.forEach { (classId, members) ->
            changes.add(ChangeInfo.MembersChanged(classId.asSingleFqName(), members))
        }
        packageMembers.forEach { (packageFqName, members) ->
            changes.add(ChangeInfo.MembersChanged(packageFqName, members))
        }
        return changes
    }

    private fun DirtyData.toChangesEither(): ChangesEither.Known {
        return ChangesEither.Known(
            lookupSymbols = dirtyLookupSymbols,
            fqNames = dirtyClassesFqNames + dirtyClassesFqNamesForceRecompile
        )
    }

    /**
     * Helper function to force exhaustive when for statements (see https://youtrack.jetbrains.com/issue/KT-47709).
     *
     * If the current IDE/Kotlin compiler already supports exhaustive when for statements, consider removing this function and its usages.
     */
    private fun Any.forceExhaustiveWhen() = this

    private fun processChangedJava(changedFiles: ChangedFiles.Known, caches: IncrementalJvmCachesManager): BuildAttribute? {
        val javaFiles = (changedFiles.modified + changedFiles.removed).filter(File::isJavaFile)

        for (javaFile in javaFiles) {
            if (!caches.platformCache.isTrackedFile(javaFile)) {
                if (!javaFile.exists()) {
                    // todo: can we do this more optimal?
                    reporter.info { "Could not get changed for untracked removed java file $javaFile" }
                    return BuildAttribute.JAVA_CHANGE_UNTRACKED_FILE_IS_REMOVED
                }

                val psiFile = psiFileProvider.javaFile(javaFile)
                if (psiFile !is PsiJavaFile) {
                    reporter.info { "[Precise Java tracking] Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                    return BuildAttribute.JAVA_CHANGE_UNEXPECTED_PSI
                }

                for (psiClass in psiFile.classes) {
                    val qualifiedName = psiClass.qualifiedName
                    if (qualifiedName == null) {
                        reporter.info { "[Precise Java tracking] Class with unknown qualified name in $javaFile" }
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

    override fun performWorkBeforeCompilation(compilationMode: CompilationMode, args: K2JVMCompilerArguments) {
        super.performWorkBeforeCompilation(compilationMode, args)

        if (compilationMode is CompilationMode.Incremental) {
            args.classpathAsList = listOf(args.destinationAsFile) + args.classpathAsList
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
            val moduleName = requireNotNull(args.moduleName) { "'moduleName' is null!" }
            val targetId = TargetId(moduleName, "java-production")
            val targetToCache = mapOf(targetId to caches.platformCache)
            val incrementalComponents = IncrementalCompilationComponentsImpl(targetToCache)
            register(IncrementalCompilationComponents::class.java, incrementalComponents)
            if (usePreciseJavaTracking) {
                val changesTracker = JavaClassesTrackerImpl(
                    caches.platformCache, changedUntrackedJavaClasses.toSet(),
                    compilerConfiguration.languageVersionSettings,
                )
                changedUntrackedJavaClasses.clear()
                register(JavaClassesTracker::class.java, changesTracker)
            }
        }

    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: K2JVMCompilerArguments,
        caches: IncrementalJvmCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean
    ): Pair<ExitCode, Collection<File>> {
        val compiler = K2JVMCompiler()
        val freeArgsBackup = args.freeArgs.toList()
        args.freeArgs += sourcesToCompile.map { it.absolutePath }
        args.allowNoSourceFiles = true
        val exitCode = compiler.exec(messageCollector, services, args)
        args.freeArgs = freeArgsBackup
        reportPerformanceData(compiler.defaultPerformanceManager)
        return exitCode to sourcesToCompile
    }

    override fun performWorkAfterCompilation(compilationMode: CompilationMode, exitCode: ExitCode, caches: IncrementalJvmCachesManager) {
        super.performWorkAfterCompilation(compilationMode, exitCode, caches)

        // No need to shrink and save classpath snapshot if exitCode != ExitCode.OK as the task will fail anyway
        if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled && exitCode == ExitCode.OK) {
            reporter.measure(GradleBuildTime.SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                shrinkAndSaveClasspathSnapshot(
                    compilationWasIncremental = compilationMode is CompilationMode.Incremental, classpathChanges, caches.lookupCache,
                    currentClasspathSnapshot, shrunkCurrentClasspathAgainstPreviousLookups, ClasspathSnapshotBuildReporter(reporter)
                )
            }
        }
    }
}
