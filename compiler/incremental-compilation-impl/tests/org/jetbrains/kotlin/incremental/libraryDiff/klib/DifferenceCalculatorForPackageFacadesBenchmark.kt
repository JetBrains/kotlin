/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForClass
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacades
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.ProtoCompareGenerated
import org.jetbrains.kotlin.incremental.hashCode
import org.jetbrains.kotlin.incremental.typeTableOrNull
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.type
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Compares the time and allocations per call of [DifferenceCalculatorForPackageFacades] and [DifferenceCalculatorForPackageFacade]
 * on the same single package part.
 *
 * One JVM measures one calculator, selected by the `KOTLIN_IC_BENCHMARK` environment variable: `A` is the baseline, `B` is the new
 * calculator. Without the variable the test is skipped. The calculators are not mixed, because both pass their own function objects
 * to the same generated `hashCode` functions, and the JIT inlines those calls only while it sees few receiver classes.
 * [DifferenceCalculatorForClass] is warmed up in both JVMs, because in production it runs in the same process and shares those call
 * sites (and its function classes) with the baseline.
 *
 * One JVM run:
 * ```
 * KOTLIN_IC_BENCHMARK=A ./gradlew :compiler:incremental-compilation-impl:test \
 *     --tests "org.jetbrains.kotlin.incremental.libraryDiff.klib.DifferenceCalculatorForPackageFacadesBenchmark" \
 *     -Pkotlin.test.xmx=4g -Pkotlin.test.xms=4g -Pkotlin.test.instrumentation.disable=true \
 *     --rerun --no-configuration-cache -q
 * ```
 *
 * A verdict needs 5 `A` and 5 `B` runs. Each run prints `BENCH|` rows and writes
 * `build/benchmarks/v3/pf3-<arm>-<timestamp>.csv`. `KOTLIN_IC_BENCHMARK_SMOKE=true` shortens every phase to check the plumbing
 * and writes to `build/benchmarks/v3/smoke` instead.
 */
@EnabledIfEnvironmentVariable(named = "KOTLIN_IC_BENCHMARK", matches = "A|B")
class DifferenceCalculatorForPackageFacadesBenchmark : KlibSnapshotTestCommon() {

    private enum class Arm { A, B }

    private class Edit(
        val changed: Set<Int> = emptySet(),
        val all: Boolean = false,
        val addedProperty: Boolean = false,
        val removed: Int? = null,
        val changedPrivate: Int? = null,
        val classChanged: Boolean = false,
    )

    private class Scenario(
        val name: String,
        val counted: Boolean,
        val old: PackagePartProtoData,
        val new: PackagePartProtoData,
        val expectedNames: Set<String>,
        val expectedGate: String,
        val expectedBucket: Int? = null,
    ) {
        val oldList = listOf(old)
        val newList = listOf(new)
        val expectedSize = expectedNames.size
        val flags = sortedSetOf<String>()

        var batch = 0
        val rounds = ArrayList<ArrayList<Double>>()
        var totalNs = 0L
        var totalCalls = 0L
        var bytesPerCall = 0L
        var gcCount = 0L
        var gcMs = 0L
        var jitMs = 0L
    }

    private val arm = Arm.valueOf(System.getenv("KOTLIN_IC_BENCHMARK") ?: "A")
    private val smoke = System.getenv("KOTLIN_IC_BENCHMARK_SMOKE") == "true"

    private val classBlockNanos = if (smoke) 200_000_000L else 2_000_000_000L
    private val classSecondBlockNanos = if (smoke) 100_000_000L else 500_000_000L
    private val warmUpNanos = if (smoke) 200_000_000L else 3_000_000_000L
    private val warmUpMinCalls = if (smoke) 2L else 50L
    private val secondPassNanos = if (smoke) 50_000_000L else 500_000_000L
    private val minSampleNanos = if (smoke) 5_000_000L else 20_000_000L
    private val samplesPerRound = if (smoke) 3 else 10
    private val pauseMillis = if (smoke) 20L else 200L

    private val threadMXBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    private val compilationMXBean = ManagementFactory.getCompilationMXBean()
    private val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()

    private val header = mutableListOf<String>()
    private val rows = mutableListOf<String>()
    private val failures = mutableListOf<String>()
    private var sink = 0L

    private lateinit var classPairs: List<Pair<ClassProtoData, ClassProtoData>>

    @Test
    fun benchmark() {
        val runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val runtime = ManagementFactory.getRuntimeMXBean()
        info("run", runId)
        info("arm", arm.name)
        info("smoke", smoke.toString())
        info("commit", gitCommit())
        info("sourcesHash", productionSourcesHash())
        info("jvm", "${runtime.vmName} ${runtime.vmVersion}")
        info("jvmArgs", runtime.inputArguments.joinToString(" "))
        info("maxHeapMb", (Runtime.getRuntime().maxMemory() shr 20).toString())
        info("processors", Runtime.getRuntime().availableProcessors().toString())

        val buildStart = System.nanoTime()
        val scenarios = buildScenarios()
        info("buildMs", ((System.nanoTime() - buildStart) / 1_000_000).toString())

        for (scenario in scenarios) checkGate(scenario)
        for (scenario in scenarios) checkOwnResult(scenario)
        failIfInvalid()

        System.gc()
        Thread.sleep(pauseMillis)

        val warmUpStart = System.nanoTime()
        val classNames = warmUpClassCalculator(classBlockNanos)
        // Otherwise the class calculator stops at its positional check and never reaches the shared `hashCode` call sites.
        check(classNames.any { it.startsWith("f") } && classNames.any { it.startsWith("p") }) {
            "The class calculator warm-up does not diff functions and properties: $classNames"
        }
        for (scenario in scenarios) warmUp(scenario, warmUpNanos, warmUpMinCalls)
        warmUpClassCalculator(classSecondBlockNanos)
        for (scenario in scenarios) warmUp(scenario, secondPassNanos, minCalls = 1)
        info("warmUpMs", ((System.nanoTime() - warmUpStart) / 1_000_000).toString())

        val measureStart = System.nanoTime()
        for (scenario in scenarios) scenario.batch = calibrate(scenario)
        repeat(ROUNDS) {
            for (scenario in scenarios) measureRound(scenario)
        }
        for (scenario in scenarios) scenario.bytesPerCall = allocatedBytesPerCall(scenario)
        info("measureMs", ((System.nanoTime() - measureStart) / 1_000_000).toString())
        info("sink", sink.toString())

        // From here on the other calculator and foreign function objects may run: nothing is timed anymore.
        for (scenario in scenarios) {
            checkBothCalculators(scenario)
            checkBuckets(scenario)
        }

        for (scenario in scenarios) addRow(runId, scenario)
        failIfInvalid()
        writeCsv(runId)
    }

    private fun runBatch(scenario: Scenario, calls: Int): Long {
        var result = 0L
        if (arm == Arm.B) {
            repeat(calls) {
                result += DifferenceCalculatorForPackageFacades(scenario.oldList, scenario.newList).difference().changedMembersNames.size
            }
        } else {
            repeat(calls) {
                result += DifferenceCalculatorForPackageFacade(scenario.old, scenario.new).difference().changedMembersNames.size
            }
        }
        return result
    }

    private fun checkedBatch(scenario: Scenario, calls: Int): Long {
        val result = runBatch(scenario, calls)
        if (result != calls.toLong() * scenario.expectedSize && "SINK" !in scenario.flags) {
            fail(scenario, "SINK", "$result != $calls calls * ${scenario.expectedSize}")
        }
        return result
    }

    private fun fail(scenario: Scenario, flag: String, message: String) {
        scenario.flags += flag
        failures += "${scenario.name}: $flag: $message"
        println("BENCH-$flag|${scenario.name}|$message")
    }

    private fun failIfInvalid() {
        check(failures.isEmpty()) { "Benchmark inputs or results are not valid, no CSV written:\n" + failures.joinToString("\n") }
    }

    /**
     * Where the baseline's positional check stops for each kind: `F300/600` is function index 300 of 600, `-` means no unequal member,
     * `count` means that the member counts differ. Uses only `checkEquals`, so it does not touch the `hashCode` call sites.
     */
    private fun gateStops(old: PackagePartProtoData, new: PackagePartProtoData): String {
        val compare = ProtoCompareGenerated(old.nameResolver, new.nameResolver, old.proto.typeTableOrNull, new.proto.typeTableOrNull)

        fun <T> stop(kind: String, oldMembers: List<T>, newMembers: List<T>, equal: (T, T) -> Boolean): String {
            if (oldMembers.size != newMembers.size) return "${kind}count"
            val index = oldMembers.indices.firstOrNull { !equal(oldMembers[it], newMembers[it]) } ?: return "$kind-"
            return "$kind$index/${oldMembers.size}"
        }

        return listOf(
            stop("F", old.proto.functionList, new.proto.functionList) { a, b -> compare.checkEquals(a, b) },
            stop("P", old.proto.propertyList, new.proto.propertyList) { a, b -> compare.checkEquals(a, b) },
            stop("T", old.proto.typeAliasList, new.proto.typeAliasList) { a, b -> compare.checkEquals(a, b) },
        ).joinToString(" ")
    }

    private fun checkGate(scenario: Scenario) {
        val actual = gateStops(scenario.old, scenario.new)
        info("gate.${scenario.name}", actual)
        if (actual != scenario.expectedGate) {
            fail(scenario, "POSITION", "the change is at '$actual', planned '${scenario.expectedGate}'")
        }
    }

    private fun checkOwnResult(scenario: Scenario) {
        val names = try {
            if (arm == Arm.B) {
                DifferenceCalculatorForPackageFacades(scenario.oldList, scenario.newList).difference().changedMembersNames
            } else {
                DifferenceCalculatorForPackageFacade(scenario.old, scenario.new).difference().changedMembersNames
            }
        } catch (e: Exception) {
            fail(scenario, "UNEXPECTED", "calculator threw $e")
            return
        }
        if (names != scenario.expectedNames) {
            fail(
                scenario, "UNEXPECTED",
                "missing=${(scenario.expectedNames - names).sorted().take(20)} extra=${(names - scenario.expectedNames).sorted().take(20)}"
            )
        }
    }

    private fun checkBothCalculators(scenario: Scenario) {
        val baseline = DifferenceCalculatorForPackageFacade(scenario.old, scenario.new).difference()
        val facades = DifferenceCalculatorForPackageFacades(scenario.oldList, scenario.newList).difference()
        if (baseline != facades) {
            val onlyBaseline = (baseline.changedMembersNames - facades.changedMembersNames).sorted().take(20)
            val onlyFacades = (facades.changedMembersNames - baseline.changedMembersNames).sorted().take(20)
            fail(scenario, "MISMATCH", "onlyBaseline=$onlyBaseline onlyNew=$onlyFacades")
        }
    }

    private fun checkBuckets(scenario: Scenario) {
        val strings = Interner<String>()
        val classIds = Interner<ClassId>()

        fun largestBucket(part: PackagePartProtoData): Int {
            val nameResolver = part.nameResolver
            val typeTable = part.proto.typeTable
            val stringIndexes = { index: Int -> strings.intern(nameResolver.getString(index)) }
            val classIdIndexes = { index: Int -> classIds.intern(nameResolver.getClassId(index)) }
            val typeById = { index: Int -> typeTable.getType(index) }
            // Kinds are bucketed separately, as in both calculators.
            val hashesByKind = listOf(
                part.proto.functionList.filterNot { it.flags.isPrivate }.map { it.hashCode(stringIndexes, classIdIndexes, typeById) },
                part.proto.propertyList.filterNot { it.flags.isPrivate }.map { it.hashCode(stringIndexes, classIdIndexes, typeById) },
                part.proto.typeAliasList.filterNot { it.flags.isPrivate }.map { it.hashCode(stringIndexes, classIdIndexes, typeById) },
            )
            return hashesByKind.maxOf { hashes -> hashes.groupingBy { it }.eachCount().values.maxOrNull() ?: 0 }
        }

        val largest = maxOf(largestBucket(scenario.old), largestBucket(scenario.new))
        info("buckets.${scenario.name}", "largest=$largest")

        val expected = scenario.expectedBucket
        // One accidental pair of equal hashes is possible among 10,000 members and does not change the timing.
        if (if (expected != null) largest != expected else largest > 2) {
            fail(scenario, "NOBUCKET", "largest bucket is $largest, expected ${expected ?: "at most 2"}")
        }
    }

    private fun warmUpClassCalculator(nanos: Long): Set<String> {
        val names = HashSet<String>()
        val start = System.nanoTime()
        do {
            for ([old, new] in classPairs) {
                val changed = DifferenceCalculatorForClass(old, new).difference().changedMembersNames
                if (names.size < 1000) names += changed
                sink += changed.size
            }
        } while (System.nanoTime() - start < nanos)
        return names
    }

    private fun warmUp(scenario: Scenario, minNanos: Long, minCalls: Long) {
        val start = System.nanoTime()
        var calls = 0L
        do {
            sink += checkedBatch(scenario, 1)
            calls++
        } while (System.nanoTime() - start < minNanos || calls < minCalls)
    }

    private fun calibrate(scenario: Scenario): Int {
        var calls = 1
        while (true) {
            val start = System.nanoTime()
            sink += checkedBatch(scenario, calls)
            val elapsed = System.nanoTime() - start
            if (elapsed >= minSampleNanos) return calls
            if (elapsed >= minSampleNanos / 4) {
                return (calls.toDouble() * minSampleNanos / elapsed).toInt() + 1
            }
            calls *= 2
        }
    }

    private fun measureRound(scenario: Scenario) {
        System.gc()
        Thread.sleep(pauseMillis)
        // The first batch after the pause is slower.
        sink += checkedBatch(scenario, scenario.batch)

        val gcCountBefore = gcMXBeans.sumOf { it.collectionCount }
        val gcTimeBefore = gcMXBeans.sumOf { it.collectionTime }
        val jitBefore = compilationMXBean.totalCompilationTime

        val samples = ArrayList<Double>()
        repeat(samplesPerRound) {
            val start = System.nanoTime()
            val batchSink = checkedBatch(scenario, scenario.batch)
            val elapsed = System.nanoTime() - start
            sink += batchSink
            samples += elapsed.toDouble() / scenario.batch
            scenario.totalNs += elapsed
            scenario.totalCalls += scenario.batch
        }
        scenario.rounds += samples

        scenario.gcCount += gcMXBeans.sumOf { it.collectionCount } - gcCountBefore
        scenario.gcMs += gcMXBeans.sumOf { it.collectionTime } - gcTimeBefore
        scenario.jitMs += compilationMXBean.totalCompilationTime - jitBefore
    }

    private fun allocatedBytesPerCall(scenario: Scenario): Long {
        val threadId = Thread.currentThread().id
        val samples = (1..3).map {
            val before = threadMXBean.getThreadAllocatedBytes(threadId)
            val batchSink = runBatch(scenario, scenario.batch)
            val after = threadMXBean.getThreadAllocatedBytes(threadId)
            sink += batchSink
            (after - before) / scenario.batch
        }
        return samples.sorted()[1]
    }

    private fun addRow(runId: String, scenario: Scenario) {
        val all = scenario.rounds.flatten()
        val firstRound = scenario.rounds.first().percentile(0.5)
        val lastRound = scenario.rounds.last().percentile(0.5)
        if (maxOf(firstRound, lastRound) / minOf(firstRound, lastRound) > 1.05) scenario.flags += "UNSTABLE"

        rows += listOf(
            runId, arm.name, scenario.name, if (scenario.counted) "yes" else "no",
            scenario.batch, all.size, scenario.totalCalls,
            (scenario.totalNs.toDouble() / scenario.totalCalls).fmt(),
            all.percentile(0.5).fmt(), all.percentile(0.1).fmt(), all.percentile(0.9).fmt(),
            firstRound.fmt(), lastRound.fmt(),
            scenario.bytesPerCall, scenario.gcCount, scenario.gcMs, scenario.jitMs,
            scenario.expectedSize, scenario.expectedGate, scenario.flags.joinToString("+"),
        ).joinToString(",")
        println("BENCH|" + rows.last().replace(',', '|'))
    }

    private fun info(key: String, value: String) {
        header += "# $key: $value"
        println("BENCH#|$key|$value")
    }

    private fun writeCsv(runId: String) {
        val buildDir = checkNotNull(System.getenv("PROJECT_BUILD_DIR")) { "PROJECT_BUILD_DIR is not set, no CSV written" }
        // Smoke files go to their own directory so that they cannot be aggregated with real runs.
        val file = File(buildDir, "benchmarks/v3/${if (smoke) "smoke/" else ""}pf3-${arm.name}-$runId.csv")
        file.parentFile.mkdirs()
        file.writeText((header + CSV_COLUMNS + rows).joinToString("\n", postfix = "\n"))
        println("BENCH#|csv|$file")
    }

    private fun projectRoot(): File? = System.getenv("PROJECT_BUILD_DIR")?.let { File(it).parentFile.parentFile.parentFile }

    private fun gitCommit(): String =
        try {
            val process = ProcessBuilder("git", "rev-parse", "HEAD").directory(projectRoot()).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
        } catch (e: Exception) {
            "unknown"
        }

    /** The two measured files have uncommitted changes, so the commit alone does not identify what was measured. */
    private fun productionSourcesHash(): String =
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            for (name in listOf("protoDifferenceUtils.kt", "ProtoCompareGenerated.kt")) {
                digest.update(File(projectRoot(), "build-common/src/org/jetbrains/kotlin/incremental/$name").readBytes())
            }
            digest.digest().take(8).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "unknown"
        }

    private fun buildScenarios(): List<Scenario> {
        val base = compile(
            "base",
            mapOf(
                "small" to mixSource("small", SMALL),
                "medium" to mixSource("medium", MEDIUM),
                "large" to mixSource("large", LARGE),
                "mediumprivate" to mixSource("mediumprivate", MEDIUM, privateCount = MEDIUM),
                "overloads" to overloadsSource(changed = null),
                "single" to "package bench.single\n\nfun g(a: Int): Int = TODO()\n",
                "classes" to classesSource(changed = false),
            )
        )
        fun old(pkg: String) = base.getValue(pkg).part()

        // The members to change are picked by their index in the old proto lists, which is what the baseline's positional check sees.
        fun middleFunction(pkg: String) = old(pkg).functionNames().let { it[it.size / 2] }
        fun middleProperty(pkg: String) = old(pkg).propertyNames().let { it[it.size / 2] }
        fun fewFunctions(pkg: String) = old(pkg).functionNames().let { names -> (FEW_FIRST until names.size step FEW_STEP).map { names[it] } }
        fun removedAlias(pkg: String) = old(pkg).proto.typeAliasList.last().let { old(pkg).nameResolver.getString(it.name) }

        val privateFunctions = old("mediumprivate").proto.functionList
        val privateIndex = (privateFunctions.size / 2 until privateFunctions.size).first { privateFunctions[it].flags.isPrivate }
        val privateName = old("mediumprivate").functionNames()[privateIndex]

        val overloadIndex = OVERLOADS / 2
        val overloadNumber = old("overloads").parameterTypes()[overloadIndex].fold(0) { number, type ->
            number * 10 + OVERLOAD_TYPES.indexOf(type).also { check(it >= 0) { "Unknown overload parameter type $type" } }
        }

        val oneFunction = compile(
            "oneFunction",
            mapOf(
                "small" to mixSource("small", SMALL, Edit(changed = setOf(middleFunction("small").memberNumber()))),
                "medium" to mixSource("medium", MEDIUM, Edit(changed = setOf(middleFunction("medium").memberNumber()))),
                "large" to mixSource("large", LARGE, Edit(changed = setOf(middleFunction("large").memberNumber()))),
                "overloads" to overloadsSource(changed = overloadNumber),
            )
        )
        val oneOther = compile(
            "oneOther",
            mapOf(
                "medium" to mixSource("medium", MEDIUM, Edit(changed = setOf(middleProperty("medium").memberNumber()))),
                "mediumprivate" to mixSource(
                    "mediumprivate", MEDIUM, Edit(changedPrivate = privateName.memberNumber()), privateCount = MEDIUM,
                ),
            )
        )
        fun fewEdit(pkg: String) = Edit(
            changed = fewFunctions(pkg).mapTo(HashSet()) { it.memberNumber() },
            addedProperty = true,
            removed = removedAlias(pkg).memberNumber(),
        )
        val fewChanged = compile(
            "fewChanged",
            mapOf(
                "medium" to mixSource("medium", MEDIUM, fewEdit("medium")),
                "large" to mixSource("large", LARGE, fewEdit("large")),
            )
        )
        val allChanged = compile(
            "allChanged",
            mapOf(
                "small" to mixSource("small", SMALL, Edit(all = true)),
                "medium" to mixSource("medium", MEDIUM, Edit(all = true)),
                "large" to mixSource("large", LARGE, Edit(all = true)),
                "classes" to classesSource(changed = true),
            )
        )
        val classOnly = compile(
            "classOnly",
            mapOf(
                "medium" to mixSource("medium", MEDIUM, Edit(classChanged = true)),
                "large" to mixSource("large", LARGE, Edit(classChanged = true)),
            )
        )

        val newClasses = allChanged.getValue("classes").classes()
        classPairs = base.getValue("classes").classes().map { [classId, old] -> old to newClasses.getValue(classId) }
        check(classPairs.size == CLASSES) { "Expected $CLASSES classes for the class calculator warm-up, got ${classPairs.size}" }

        fun scenario(
            name: String, counted: Boolean, pkg: String, new: Map<String, KlibFileInfo>, expectedNames: Set<String>, expectedGate: String,
        ): Scenario {
            val oldInfo = base.getValue(pkg)
            val newInfo = new.getValue(pkg)
            check(!oldInfo.packageFragmentProto.contentEquals(newInfo.packageFragmentProto)) { "$name: the fragment did not change" }
            return Scenario(name, counted, oldInfo.part(), newInfo.part(), expectedNames, expectedGate)
        }

        fun oneFunctionGate(pkg: String) = old(pkg).proto.functionCount.let { "F${it / 2}/$it P- T-" }
        fun fewNames(pkg: String) = fewFunctions(pkg).toSet() + ADDED_PROPERTY + removedAlias(pkg)
        fun fewGate(pkg: String) = "F$FEW_FIRST/${old(pkg).proto.functionCount} Pcount Tcount"
        fun allNames(pkg: String) = old(pkg).let { it.functionNames() + it.propertyNames() + it.typeAliasNames() }.toSet()
        val allGate = { pkg: String -> old(pkg).proto.let { "F0/${it.functionCount} P0/${it.propertyCount} T0/${it.typeAliasCount}" } }
        val unchangedMedium = base.getValue("medium").let { KlibFileInfo(it.packageName, it.packageFragmentProto) }.part()

        val scenarios = listOf(
            scenario("M-1F", true, "medium", oneFunction, setOf(middleFunction("medium")), oneFunctionGate("medium")),
            scenario(
                "M-1P", true, "medium", oneOther, setOf(middleProperty("medium")),
                old("medium").proto.propertyCount.let { "F- P${it / 2}/$it T-" },
            ),
            scenario("M-FEW", true, "medium", fewChanged, fewNames("medium"), fewGate("medium")),
            scenario("M-ALL", true, "medium", allChanged, allNames("medium"), allGate("medium")),
            scenario("M-PRIV", true, "mediumprivate", oneOther, emptySet(), "F$privateIndex/${privateFunctions.size} P- T-"),
            scenario("M-CLS", true, "medium", classOnly, emptySet(), "F- P- T-"),
            scenario("L-1F", true, "large", oneFunction, setOf(middleFunction("large")), oneFunctionGate("large")),
            scenario("L-FEW", true, "large", fewChanged, fewNames("large"), fewGate("large")),
            scenario("L-ALL", true, "large", allChanged, allNames("large"), allGate("large")),
            scenario("L-CLS", true, "large", classOnly, emptySet(), "F- P- T-"),
            scenario("O-1", true, "overloads", oneFunction, setOf("f"), "F$overloadIndex/$OVERLOADS P- T-"),
            scenario("S-1F", false, "small", oneFunction, setOf(middleFunction("small")), oneFunctionGate("small")),
            scenario("S-ALL", false, "small", allChanged, allNames("small"), allGate("small")),
            Scenario("M-U", false, old("medium"), unchangedMedium, emptySet(), "F- P- T-"),
            Scenario(
                "C-X", false, collidingFunctions(old("single")), collidingFunctions(old("single"), reversed = true),
                emptySet(), "F0/$COLLIDING P- T-", COLLIDING,
            ),
        )

        for ([pkg, size] in listOf("small" to SMALL, "medium" to MEDIUM, "large" to LARGE, "mediumprivate" to MEDIUM)) {
            check(old(pkg).memberCount(private = false) == size) { "Unexpected member count in $pkg" }
        }
        for (part in listOf(old("mediumprivate"), oneOther.getValue("mediumprivate").part())) {
            check(part.memberCount(private = true) == MEDIUM) { "Private members are missing" }
        }
        check(old("overloads").proto.functionCount == OVERLOADS) { "Unexpected overload count" }
        check(scenarios.single { it.name == "M-U" }.let { it.old !== it.new && it.old.proto !== it.new.proto }) { "M-U reuses the part" }

        return scenarios
    }

    /**
     * Version requirements enter the member hash as raw ints, so the list `[j, K - 31 * j]` adds `31 * (31 * h + j) + K - 31 * j`,
     * which does not depend on `j`: the copies are different members with one hash.
     */
    private fun collidingFunctions(single: PackagePartProtoData, reversed: Boolean = false): PackagePartProtoData {
        val function = single.proto.functionList.single()
        val copies = (0 until COLLIDING).map { j ->
            function.toBuilder().clearVersionRequirement().addVersionRequirement(j).addVersionRequirement(31 * COLLIDING - 31 * j).build()
        }
        val proto = single.proto.toBuilder().clearFunction().addAllFunction(if (reversed) copies.reversed() else copies).build()
        return PackagePartProtoData(proto, single.nameResolver, single.packageFqName)
    }

    private fun compile(name: String, sources: Map<String, String>): Map<String, KlibFileInfo> {
        val srcDir = File(tmpDir, "src/$name")
        for (entry in sources) {
            File(srcDir, "${entry.key}.kt").apply { parentFile.mkdirs() }.writeText(entry.value)
        }
        File(srcDir, "types.kt").writeText(
            "package $TYPES_PACKAGE\n\n" + (0 until USER_CLASSES).joinToString("\n") { "class U$it" } + "\n"
        )
        val byPackage = snapshotKlib(srcDir).klibFileSnapshots.groupBy { it.packageName.asString() }
        return sources.keys.associateWith { pkg ->
            val fragments = byPackage["bench.$pkg"].orEmpty()
            check(fragments.size == 1) { "Expected one fragment for bench.$pkg in $name, got ${fragments.size}" }
            fragments.single().classMemberLevelSnapshot!!
        }
    }

    private fun KlibFileInfo.part(): PackagePartProtoData = proto.filterIsInstance<PackagePartProtoData>().single()

    private fun KlibFileInfo.classes(): Map<ClassId, ClassProtoData> =
        proto.filterIsInstance<ClassProtoData>().associateBy { it.nameResolver.getClassId(it.proto.fqName) }

    private val Int.isPrivate: Boolean get() = Flags.VISIBILITY.get(this) == ProtoBuf.Visibility.PRIVATE

    private fun PackagePartProtoData.functionNames(): List<String> = proto.functionList.map { nameResolver.getString(it.name) }
    private fun PackagePartProtoData.propertyNames(): List<String> = proto.propertyList.map { nameResolver.getString(it.name) }
    private fun PackagePartProtoData.typeAliasNames(): List<String> = proto.typeAliasList.map { nameResolver.getString(it.name) }

    private fun String.memberNumber(): Int = filter { it.isDigit() }.toInt()

    private fun PackagePartProtoData.memberCount(private: Boolean): Int =
        (proto.functionList.map { it.flags } + proto.propertyList.map { it.flags } + proto.typeAliasList.map { it.flags })
            .count { it.isPrivate == private }

    private fun PackagePartProtoData.parameterTypes(): List<List<String>> {
        val typeTable = TypeTable(proto.typeTable)
        return proto.functionList.map { function ->
            function.valueParameterList.map { nameResolver.getQualifiedClassName(it.type(typeTable).className).substringAfterLast('/') }
        }
    }

    private fun mixSource(pkg: String, n: Int, edit: Edit = Edit(), privateCount: Int = 0): String = buildString {
        appendLine("package bench.$pkg")
        appendLine()
        appendLine("import $TYPES_PACKAGE.*")
        appendLine()
        if (edit.addedProperty) appendLine("val $ADDED_PROPERTY: Int get() = TODO()")

        for (i in 0 until n) {
            if (i != edit.removed) {
                appendLine(member(i, prefix = "", changed = edit.all || i in edit.changed, private = false))
            }
            if (i < privateCount) {
                appendLine(member(i, prefix = "x", changed = i == edit.changedPrivate, private = true))
            }
        }

        // A class in the same file: its change makes the fragment differ while the package members stay equal.
        appendLine("class Marker { fun m(): ${if (edit.classChanged) "Long" else "Int"} = TODO() }")
    }

    private fun classesSource(changed: Boolean): String = buildString {
        appendLine("package bench.classes")
        appendLine()
        appendLine("import $TYPES_PACKAGE.*")
        appendLine()
        for (c in 0 until CLASSES) {
            appendLine("class K$c {")
            // Members 0..8 of a block of ten are functions and properties; 9 is a type alias, which a class cannot hold.
            for (k in 0 until 9) appendLine("    " + member(c * 10 + k, prefix = "", changed = changed, private = false).replace("\n", "\n    "))
            appendLine("}")
        }
    }

    private fun member(i: Int, prefix: String, changed: Boolean, private: Boolean): String {
        val modifier = if (private) "private " else ""
        fun type(base: String, other: String) = if (changed) other else base
        val userClass = "U${(if (changed) i + 1 else i) % USER_CLASSES}"
        val shape = i % 10 + i / 10

        return when (i % 10) {
            in 0..5 -> {
                val name = "${prefix}f$i"
                when (shape % 6) {
                    0 -> "${modifier}fun $name(a: Int, b: String): $userClass = TODO()"
                    1 -> "${modifier}fun $name(a: List<String>, b: Map<String, Int>?): ${type("String?", "String")} = TODO()"
                    2 -> "${modifier}fun String.$name(a: $userClass): Boolean = TODO()"
                    3 -> "${modifier}fun <T : Comparable<T>> $name(a: T, b: List<T>): ${type("T", "T?")} = TODO()"
                    4 -> "${modifier}fun $name(a: (Int) -> String, vararg b: Double): ${type("Unit", "Int")} = TODO()"
                    else -> "${modifier}fun $name(a: Int = 0, b: Array<String>? = null): ${type("Long", "Short")} = TODO()"
                }
            }
            in 6..8 -> {
                val name = "${prefix}p$i"
                when (shape % 3) {
                    0 -> "${modifier}val $name: $userClass get() = TODO()"
                    1 -> "${modifier}var $name: ${type("String", "Int")}\n    get() = TODO()\n    set(value) {}"
                    else -> "${modifier}val String.$name: ${type("List<String>", "Set<String>")} get() = TODO()"
                }
            }
            else -> {
                val name = "${prefix}T$i"
                when (shape % 3) {
                    0 -> "${modifier}typealias $name = List<$userClass>"
                    1 -> "${modifier}typealias $name = ${type("Map<String, Int>", "Map<String, Long>")}"
                    else -> "${modifier}typealias $name = ${type("(Int) -> String", "(Int) -> Int")}"
                }
            }
        }
    }

    private fun overloadsSource(changed: Int?): String = buildString {
        appendLine("package bench.overloads")
        appendLine()
        for (number in 0 until OVERLOADS) {
            val a = OVERLOAD_TYPES[number / 100]
            val b = OVERLOAD_TYPES[number / 10 % 10]
            val c = OVERLOAD_TYPES[number % 10]
            appendLine("fun f(a: $a, b: $b, c: $c): ${if (number == changed) "Long" else "Int"} = TODO()")
        }
    }

    private fun List<Double>.percentile(p: Double): Double {
        val sorted = sorted()
        val position = p * (sorted.size - 1)
        val lower = position.toInt()
        val upper = minOf(lower + 1, sorted.size - 1)
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower)
    }

    private fun Double.fmt(): String = String.format(Locale.ROOT, "%.1f", this)

    private companion object {
        const val SMALL = 10
        const val MEDIUM = 1_000
        const val LARGE = 10_000
        const val OVERLOADS = 1_000
        const val COLLIDING = 200
        const val CLASSES = 50

        // One changed function per 100 members (60 functions), the first one in the middle of the first block.
        const val FEW_FIRST = 30
        const val FEW_STEP = 60
        const val ADDED_PROPERTY = "addedAtTop"

        // Kept below 31 class ids in a package together with the `kotlin.*` types of the mix: with 31 or more,
        // `31 * idx(name) + idx(class)` can repeat for different members.
        const val USER_CLASSES = 10

        // A deep package, because the new calculator rebuilds the class id from the qualified name table on every lookup.
        const val TYPES_PACKAGE = "bench.types.a.b.c"

        const val ROUNDS = 3

        val OVERLOAD_TYPES = listOf("Int", "Long", "Short", "Byte", "Double", "Float", "Char", "Boolean", "String", "Any")

        const val CSV_COLUMNS = "run,arm,scenario,counted,batch,samples,calls,mean_ns,median_ns,p10_ns,p90_ns,round1_ns,round3_ns," +
                "bytes_per_call,gc_count,gc_ms,jit_ms,result_size,gate_stop,flags"
    }
}
