/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotDisabled
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.ClasspathChanges.NotAvailableForJSCompiler
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClasspathChanges
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ProgramSymbolSet
import org.jetbrains.kotlin.incremental.classpathDiff.shrinkAndSaveClasspathSnapshot
import org.jetbrains.kotlin.incremental.dirtyFiles.DirtyFilesContainer
import org.jetbrains.kotlin.incremental.dirtyFiles.getClasspathChanges
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.snapshots.LazyClasspathSnapshot
import org.jetbrains.kotlin.incremental.util.Either
import java.io.File

open class IncrementalJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    buildHistoryFile: File?,
    outputDirs: Collection<File>?,
    private val modulesApiHistory: ModulesApiHistory,
    kotlinSourceFilesExtensions: Set<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    private val classpathChanges: ClasspathChanges,
    icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
) : IncrementalJvmCompilerRunnerBase(
    workingDir = workingDir,
    reporter = reporter,
    buildHistoryFile = buildHistoryFile,
    outputDirs = outputDirs,
    kotlinSourceFilesExtensions = kotlinSourceFilesExtensions,
    icFeatures = icFeatures,
) {
    override val shouldTrackChangesInLookupCache
        get() = classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun

    override fun calculateSourcesToCompile(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode {
        return try {
            calculateSourcesToCompileImpl(caches, changedFiles, args, messageCollector, classpathAbiSnapshots)
        } finally {
            this.messageCollector.forward(messageCollector)
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

    private val lazyClasspathSnapshot = LazyClasspathSnapshot(classpathChanges, ClasspathSnapshotBuildReporter(reporter))

    private fun changedAndImpactedSymbolsBasedOnClasspathSnapshot(caches: IncrementalJvmCachesManager): ChangesEither {
        return when (classpathChanges) {
            // Note: classpathChanges is deserialized, so they are no longer singleton objects and need to be compared using `is` (not `==`)
            is NoChanges -> ChangesEither.Known(emptySet(), emptySet())
            is ToBeComputedByIncrementalCompiler -> reporter.measure(GradleBuildTime.COMPUTE_CLASSPATH_CHANGES) {
                reporter.addMetric(GradleBuildPerformanceMetric.COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT, 1)
                val classpathChanges = computeClasspathChanges(
                    caches.lookupCache,
                    lazyClasspathSnapshot,
                    ClasspathSnapshotBuildReporter(reporter)
                )
                // `classpathChanges` contains changed and impacted symbols on the classpath.
                // We also need to compute symbols in the current module that are impacted by `classpathChanges`.
                classpathChanges.toChangeInfoList().getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter).toChangesEither()
            }
            is NotAvailableDueToMissingClasspathSnapshot -> ChangesEither.Unknown(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND)
            is NotAvailableForNonIncrementalRun -> ChangesEither.Unknown(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ClasspathSnapshotDisabled -> error("Unexpected ClasspathSnapshotDisabled in this path")
            is NotAvailableForJSCompiler -> error("Unexpected type for this code path: ${classpathChanges.javaClass.name}.")
        }
    }

    private fun verifyBuildHistoryFilesState(): BuildAttribute? {
        if (buildHistoryFile == null) {
            error("The build is configured to use the build-history based IC approach, but doesn't specify the buildHistoryFile")
        }
        if (!icFeatures.withAbiSnapshot && buildHistoryFile.isFile != true) {
            // If the previous build was a Gradle cache hit, the build history file must have been deleted as it is marked as
            // @LocalState in the Gradle task. Therefore, this compilation will need to run non-incrementally.
            // (Note that buildHistoryFile is outside workingDir. We don't need to perform the same check for files inside
            // workingDir as workingDir is an @OutputDirectory, so the files must be present in an incremental build.)
            return BuildAttribute.NO_BUILD_HISTORY
        }
        if (!lastBuildInfoFile.exists()) {
            return BuildAttribute.NO_LAST_BUILD_INFO
        }
        return null // no rebuild necessary so far, compilation might proceed
    }

    private fun changedAndImpactedSymbolsBasedOnHistoryFiles(
        caches: IncrementalJvmCachesManager,
        args: K2JVMCompilerArguments,
        changedFiles: DeterminableFiles.Known,
        abiSnapshots: Map<String, AbiSnapshot>,
        lastBuildInfo: BuildInfo
    ): ChangesEither {
        reporter.debug { "Last Kotlin Build info -- $lastBuildInfo" }
        val scopes = caches.lookupCache.lookupSymbols.map { it.scope.ifBlank { it.name } }.distinct()

        // The old IC currently doesn't compute impacted symbols? KT-75180
        return getClasspathChanges(
            args.classpathAsList,
            changedFiles,
            lastBuildInfo,
            modulesApiHistory,
            reporter,
            abiSnapshots,
            icFeatures.withAbiSnapshot,
            caches.platformCache,
            scopes
        )
    }

    private fun calculateSourcesToCompileImpl(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector,
        abiSnapshots: Map<String, AbiSnapshot>,
    ): CompilationMode {
        reporter.debug { "Classpath changes info passed from Gradle task: ${classpathChanges::class.simpleName}" }
        val changedAndImpactedSymbols = when (classpathChanges) {
            is ClasspathSnapshotDisabled -> reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_DEPENDENCIES) {
                verifyBuildHistoryFilesState()?.let { rebuildReason ->
                    return CompilationMode.Rebuild(rebuildReason)
                }
                val lastBuildInfo = BuildInfo.read(lastBuildInfoFile, messageCollector)
                    ?: return CompilationMode.Rebuild(BuildAttribute.INVALID_LAST_BUILD_INFO)
                changedAndImpactedSymbolsBasedOnHistoryFiles(
                    caches,
                    args,
                    changedFiles,
                    abiSnapshots,
                    lastBuildInfo
                )
            }
            else -> changedAndImpactedSymbolsBasedOnClasspathSnapshot(caches)
        }

        return when (changedAndImpactedSymbols) {
            is ChangesEither.Unknown -> {
                reporter.info { "Could not get classpath changes: ${changedAndImpactedSymbols.reason}" }
                CompilationMode.Rebuild(changedAndImpactedSymbols.reason)
            }
            is ChangesEither.Known -> {
                calculateSourcesToCompileWithKnownChanges(
                    caches,
                    changedFiles,
                    changedAndImpactedSymbols
                )
            }
        }
    }

    private fun calculateSourcesToCompileWithKnownChanges(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        changedAndImpactedSymbols: ChangesEither.Known,
    ): CompilationMode {
        val dirtyFiles = DirtyFilesContainer(caches, reporter, kotlinSourceFilesExtensions)
        initDirtyFiles(dirtyFiles, changedFiles)

        dirtyFiles.addByDirtySymbols(changedAndImpactedSymbols.lookupSymbols)
        dirtyFiles.addByDirtyClasses(changedAndImpactedSymbols.fqNames)

        reporter.measure(GradleBuildTime.IC_ANALYZE_CHANGES_IN_JAVA_SOURCES) {
            val javaRebuildReason = javaInteropCoordinator.analyzeChangesInJavaSources(caches, changedFiles, dirtyFiles)
            if (javaRebuildReason != null) {
                return javaRebuildReason
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

    private fun processLookupSymbolsForAndroidLayouts(changedFiles: DeterminableFiles.Known): Collection<LookupSymbol> {
        val result = mutableListOf<LookupSymbol>()
        for (file in changedFiles.modified + changedFiles.removed) {
            if (file.extension.lowercase() != "xml") continue
            val layoutName = file.name.substringBeforeLast('.')
            result.add(LookupSymbol(ANDROID_LAYOUT_CONTENT_LOOKUP_NAME, layoutName))
        }

        return result
    }

    override fun performWorkAfterCompilation(compilationMode: CompilationMode, exitCode: ExitCode, caches: IncrementalJvmCachesManager) {
        super.performWorkAfterCompilation(compilationMode, exitCode, caches)

        // No need to shrink and save classpath snapshot if exitCode != ExitCode.OK as the task will fail anyway
        if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled && exitCode == ExitCode.OK) {
            reporter.measure(GradleBuildTime.SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                shrinkAndSaveClasspathSnapshot(
                    compilationWasIncremental = compilationMode is CompilationMode.Incremental, classpathChanges, caches.lookupCache,
                    lazyClasspathSnapshot, ClasspathSnapshotBuildReporter(reporter)
                )
            }
        }
    }
}
