/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.*
import org.jetbrains.kotlin.incremental.snapshots.LazyClasspathSnapshot
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.openjdk.jmh.annotations.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Measures the phases of shrinking the classpath snapshot of a JVM module, which the incremental compiler does on every build where the
 * classpath snapshot is enabled (see `shrinkAndSaveClasspathSnapshot` and `ClasspathChangesComputer.computeClasspathChanges`).
 *
 * The classpath consists of the jars on the benchmark's own classpath plus the Gradle API, which gives a realistically large classpath
 * (compiler, IntelliJ core, Gradle API, and their dependencies). The lookups of the "compiled module" are synthesized: a fraction of the
 * classpath classes is referenced (by name and by members) and the rest of the lookups refer to symbols of the module itself.
 */
@Suppress("unused")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsAppend = ["-Xmx6g"])
open class ClasspathSnapshotShrinkerBenchmark {

    /** Total number of distinct lookup symbols stored in the lookup cache of the compiled module. */
    @Param("200000", "1000000")
    private var lookupCount: Int = 0

    /** Number of lookup symbols added by an incremental compilation (e.g., the user adds a few calls to a source file). */
    private val addedLookupCount = 20

    private lateinit var workingDir: File
    private lateinit var classpathEntrySnapshotFiles: List<File>
    private lateinit var classpathSnapshot: ClasspathSnapshot
    private lateinit var allClasses: List<AccessibleClassSnapshot>
    private lateinit var lookupStorage: LookupStorage
    private lateinit var lookupSymbols: Collection<LookupSymbolKey>
    private lateinit var addedLookupSymbols: Set<LookupSymbolKey>
    private lateinit var shrunkClasses: List<AccessibleClassSnapshot>
    private lateinit var classpathSnapshotFiles: ClasspathSnapshotFiles
    private lateinit var shrunkClasspathSnapshotFile: File
    private val reporter = ClasspathSnapshotBuildReporter(DoNothingBuildReporter)

    @Setup(Level.Trial)
    fun setUp() {
        workingDir = Files.createTempDirectory("classpathSnapshotShrinker.benchmark").toFile()

        val classpath = (System.getProperty("java.class.path").split(File.pathSeparatorChar) +
                System.getProperty("classpathSnapshot.gradleApi").split(File.pathSeparatorChar))
            .map { File(it) }
            .filter { it.isFile && it.extension == "jar" }
            .distinct()
        val settings = ClasspathEntrySnapshotter.Settings(
            ClassSnapshotGranularity.CLASS_LEVEL, parseInlinedLocalClasses = true, expandTypeAliases = false
        )
        val snapshotsDir = File(workingDir, "snapshots").apply { mkdirs() }
        classpathEntrySnapshotFiles = classpath.mapIndexed { index, jar ->
            File(snapshotsDir, "$index-${jar.nameWithoutExtension}.bin").also {
                ClasspathEntrySnapshotExternalizer.saveToFile(it, ClasspathEntrySnapshotter.snapshot(jar, settings))
            }
        }
        classpathSnapshot = loadClasspathSnapshot()
        allClasses = classpathSnapshot.removeDuplicateAndInaccessibleClasses()

        val random = Random(42)
        val lookups = generateLookups(allClasses, random)
        val lookupSymbolsToAdd = generateProjectLookups(addedLookupCount, random, prefix = "added")

        // The lookup cache is written by a previous build...
        val lookupsDir = File(workingDir, "lookups")
        LookupStorage(lookupsDir, IncrementalCompilationContext(storeFullFqNamesInLookupCache = true)).apply {
            val paths = (0 until 2000).map { File(workingDir, "src/File$it.kt").path }
            val multiMap = MultiMap.createSet<LookupSymbol, String>()
            lookups.forEachIndexed { index, lookup -> multiMap.putValue(lookup, paths[index % paths.size]) }
            addAll(multiMap, paths.toSet())
            close()
        }
        // ... and opened by the current build with the same settings as the Kotlin Gradle plugin
        lookupStorage = LookupStorage(
            lookupsDir,
            IncrementalCompilationContext(
                storeFullFqNamesInLookupCache = true,
                trackChangesInLookupCache = true,
                icFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION.copy(keepIncrementalCompilationCachesInMemory = true),
            )
        )

        lookupSymbols = lookupStorage.lookupSymbols.toList()
        check(lookupSymbols.size == lookupCount) { "Expected $lookupCount lookups but got ${lookupSymbols.size}" }
        // The lookups added by the current (incremental) compilation; they are kept in memory as `lookupStorage` is not flushed
        val addedPath = File(workingDir, "src/Added.kt").path
        lookupStorage.addAll(MultiMap.createSet<LookupSymbol, String>().apply { lookupSymbolsToAdd.forEach { putValue(it, addedPath) } }, setOf(addedPath))
        addedLookupSymbols = lookupStorage.addedLookupSymbols
        check(addedLookupSymbols.size == addedLookupCount)

        // The shrunk classpath snapshot saved by the previous build
        shrunkClasses = ClasspathSnapshotShrinker.shrinkClasses(allClasses, lookupSymbols)
        classpathSnapshotFiles = ClasspathSnapshotFiles(classpathEntrySnapshotFiles, File(workingDir, "classpath-snapshot").apply { mkdirs() })
        shrunkClasspathSnapshotFile = classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile
        ListExternalizer(AccessibleClassSnapshotExternalizer).saveToFile(shrunkClasspathSnapshotFile, shrunkClasses)

        println(
            "\nClasspath: ${classpath.size} jars, ${allClasses.size} accessible classes " +
                    "(${classpathEntrySnapshotFiles.sumOf { it.length() } / 1024} KB of entry snapshots), " +
                    "lookups: ${lookupSymbols.size}, shrunk classpath: ${shrunkClasses.size} classes " +
                    "(${shrunkClasspathSnapshotFile.length() / 1024} KB)"
        )
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        lookupStorage.close()
        workingDir.deleteRecursively()
    }

    private fun loadClasspathSnapshot(): ClasspathSnapshot =
        ClasspathSnapshot(classpathEntrySnapshotFiles.map { ClasspathEntrySnapshotExternalizer.loadFromFile(it) })

    /** Lookups recorded by a module that references a part of the classpath. */
    private fun generateLookups(allClasses: List<AccessibleClassSnapshot>, random: Random): List<LookupSymbol> {
        val lookups = LinkedHashSet<LookupSymbol>(lookupCount)
        // Reference ~5% of the classes on the classpath: by name, and a few of their members
        val maxClasspathLookups = lookupCount / 2
        for (clazz in allClasses.shuffled(random)) {
            if (lookups.size >= maxClasspathLookups || random.nextInt(20) != 0) continue
            when (clazz) {
                is RegularKotlinClassSnapshot, is JavaClassSnapshot -> {
                    val fqName = clazz.classId.asSingleFqName()
                    lookups.add(LookupSymbol(name = fqName.shortName().asString(), scope = fqName.parent().asString()))
                    repeat(5) { lookups.add(LookupSymbol(name = "member$it", scope = fqName.asString())) }
                }
                is PackageFacadeKotlinClassSnapshot -> clazz.packageMemberNames.take(3).forEach {
                    lookups.add(LookupSymbol(name = it, scope = clazz.classId.packageFqName.asString()))
                }
                is MultifileClassKotlinClassSnapshot -> Unit
            }
        }
        // Names that are looked up in the default imports but not found there
        val defaultImports = listOf("kotlin", "kotlin.collections", "kotlin.io", "kotlin.text", "java.lang", "kotlin.jvm")
        val defaultImportLookups = lookupCount / 10
        var i = 0
        while (lookups.size < maxClasspathLookups + defaultImportLookups) {
            lookups.add(LookupSymbol(name = "projectName${i++}", scope = defaultImports[i % defaultImports.size]))
        }
        // The rest are lookups into the module itself
        lookups.addAll(generateProjectLookups(lookupCount - lookups.size, random, prefix = "project"))
        return lookups.toList()
    }

    private fun generateProjectLookups(count: Int, random: Random, prefix: String): List<LookupSymbol> =
        List(count) {
            val packageIndex = random.nextInt(300)
            LookupSymbol(name = "${prefix}Member$it", scope = "com.example.app.p$packageIndex.C${random.nextInt(50)}")
        }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // End to end, as executed by the incremental compiler (with the classpath entry snapshots cached in memory, as in a warm daemon)
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * `ToBeComputedByIncrementalCompiler` (a classpath entry has changed): the shrinking done by
     * `ClasspathChangesComputer.computeClasspathChanges` before diffing the classpath (the `SHRINK_CURRENT_CLASSPATH_SNAPSHOT` metric).
     */
    @Benchmark
    fun endToEndShrinkBeforeClasspathDiff(): List<AccessibleClassSnapshot> {
        val lazyClasspathSnapshot = LazyClasspathSnapshot(ToBeComputedByIncrementalCompiler(classpathSnapshotFiles), reporter)
        lazyClasspathSnapshot.getCurrentClasspathSnapshot(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        return lazyClasspathSnapshot.getComputedShrunkClasspathAgainstPreviousLookups(
            lookupStorage, LazySnapshotLoadingMetrics.OnClasspathDiffComputation
        )
    }

    /**
     * `NoChanges` in the classpath, and the compilation added a few lookups (the most common incremental build):
     * `shrinkAndSaveClasspathSnapshot` after compilation (the `SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION` metric).
     */
    @Benchmark
    fun endToEndShrinkAndSaveAfterIncrementalCompilation() {
        val classpathChanges = NoChanges(classpathSnapshotFiles)
        shrinkAndSaveClasspathSnapshot(
            compilationWasIncremental = true, classpathChanges, lookupStorage, LazyClasspathSnapshot(classpathChanges, reporter), reporter
        )
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // Non-incremental shrinking and shrinking before classpath diffing (`ToBeComputedByIncrementalCompiler`)
    // ---------------------------------------------------------------------------------------------------------------------------------

    /** `LookupStorage.lookupSymbols`: reads all keys of the lookup cache. */
    @Benchmark
    fun getLookupSymbols(): Int = lookupStorage.lookupSymbols.size

    /** `ClasspathSnapshotShrinker.shrinkClasses`: finds directly and transitively referenced classes. */
    @Benchmark
    fun shrinkClasses(): List<AccessibleClassSnapshot> = ClasspathSnapshotShrinker.shrinkClasses(allClasses, lookupSymbols)

    /** `ClasspathSnapshotShrinker.shrinkClasspath`: [getLookupSymbols] + [shrinkClasses]. */
    @Benchmark
    fun shrinkClasspath(): List<AccessibleClassSnapshot> = ClasspathSnapshotShrinker.shrinkClasspath(allClasses, lookupStorage)

    /** The fixed part of [shrinkClasses] that does not depend on the lookups: building the graph of impacting classes. */
    @Benchmark
    fun buildReverseImpactGraph(): Any = AllImpacts.getReverseResolver(allClasses)

    /** Shrinking with no lookups at all, i.e., the minimal cost of a [ClasspathSnapshotShrinker.shrinkClasses] call. */
    @Benchmark
    fun shrinkClassesWithoutLookups(): List<AccessibleClassSnapshot> = ClasspathSnapshotShrinker.shrinkClasses(allClasses, emptyList())

    // ---------------------------------------------------------------------------------------------------------------------------------
    // Incremental shrinking after an incremental compilation that added a few lookups (`ChangedLookupsUnchangedClasspath`)
    // ---------------------------------------------------------------------------------------------------------------------------------

    /** Same steps as `shrinkAndSaveClasspathSnapshot` with `ShrinkMode.ChangedLookups`, excluding I/O. */
    @Benchmark
    fun incrementalShrinkAfterAddingFewLookups(): List<AccessibleClassSnapshot> {
        val shrunkClassIds = shrunkClasses.mapTo(mutableSetOf()) { it.classId }
        val notYetShrunkClasses = allClasses.filter { it.classId !in shrunkClassIds }
        return shrunkClasses + ClasspathSnapshotShrinker.shrinkClasses(notYetShrunkClasses, addedLookupSymbols)
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // I/O around shrinking
    // ---------------------------------------------------------------------------------------------------------------------------------

    /** Loading the current classpath snapshot when `CachedClasspathSnapshotSerializer` has no cached entries (e.g., a fresh daemon). */
    @Benchmark
    fun loadCurrentClasspathSnapshotWithoutCache(): List<AccessibleClassSnapshot> =
        loadClasspathSnapshot().removeDuplicateAndInaccessibleClasses()

    /** `removeDuplicateAndInaccessibleClasses`: executed even when the classpath entry snapshots are cached. */
    @Benchmark
    fun removeDuplicateAndInaccessibleClasses(): List<AccessibleClassSnapshot> = classpathSnapshot.removeDuplicateAndInaccessibleClasses()

    @Benchmark
    fun loadShrunkClasspathSnapshot(): List<AccessibleClassSnapshot> =
        ListExternalizer(AccessibleClassSnapshotExternalizer).loadFromFile(shrunkClasspathSnapshotFile)

    @Benchmark
    fun saveShrunkClasspathSnapshot() {
        ListExternalizer(AccessibleClassSnapshotExternalizer).saveToFile(File(workingDir, "shrunk-copy.bin"), shrunkClasses)
    }
}
