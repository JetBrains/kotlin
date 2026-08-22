/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.kotlin.test.DynamicWithMaxThresholdParallelExecutionConfigurationStrategy
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.junit.jupiter.engine.config.CachingJupiterConfiguration
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration
import org.junit.jupiter.engine.config.JupiterConfiguration
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext
import org.junit.jupiter.engine.execution.LauncherStoreFacade
import org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory.createThrowableCollector
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.hierarchical.Node
import org.junit.platform.engine.support.hierarchical.ThrowableCollector
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Executes two-stage compiler tests ([AbstractTwoStageKotlinCompilerTestBase]) in two passes.
 *
 * The planning pass runs the test method itself for every test, which is cheap: it only builds the test
 * configurations and extracts the module structure, which is all the [org.jetbrains.kotlin.test.model.GroupingTestIsolator]s
 * need to compute a [BatchKey]. Everything the test allocated is disposed right away, so only the key is kept.
 *
 * Once all keys are known, tests with an equal key are packed into batches of at most
 * [GROUPING_BATCH_SIZE_PROP] tests. Batches are formed across the whole test suite rather than within a single
 * `@Nested` test group, which is what keeps the number of batches (and therefore the number of grouping-stage
 * compilations) low - but never across two different concrete (generated) JUnit test classes, since a [BatchKey]
 * always includes the test's class in addition to the isolator tokens: different test classes typically mean
 * different target/runner configurations for the grouping stage, which the engine has no generic way to compare,
 * so tests whose tokens happen to match regardless (most commonly, both simply `Regular`) are still never mixed.
 * Only then do the expensive stages run, batch by batch: the non-grouping stage for the tests of a batch,
 * followed by a single grouping stage for the whole batch.
 *
 * The peak number of test pipelines kept in memory undisposed is therefore bounded by [MAX_TESTS_IN_FLIGHT_PROP],
 * independently of the size of the test suite; its default is chosen to match [GROUPING_BATCH_SIZE_PROP] *
 * [SIMULTANEOUS_BATCHES_PROP], but the three are configured independently, so a suite that changes one without
 * the others gets whatever [MAX_TESTS_IN_FLIGHT_PROP] actually says, not that product.
 */
class CompilerTestGroupingTestEngine : TestEngine {
    companion object {
        const val ID = "kotlin-compiler-grouping-engine"
        private val logger = LoggerFactory.getLogger(CompilerTestGroupingTestEngine::class.java)

        private const val GROUPING_ENGINE_POOL_SIZE_PROP = "kotlin.test.grouping.engine.pool.size"

        /**
         * A hard upper bound on the number of tests which are compiled and executed together as a single batch.
         * Grouping-stage memory consumption grows with the batch size, as a batch is linked and lowered as a
         * single IR world containing the standard library and the KLib of every test of the batch.
         */
        private const val GROUPING_BATCH_SIZE_PROP = "kotlin.test.grouping.engine.batch.size"
        private const val DEFAULT_GROUPING_BATCH_SIZE = 50

        /**
         * An upper bound on the number of tests whose pipelines may be alive (compiled, but not yet disposed)
         * at the same time. This is what actually bounds the memory consumption of a run, as a test keeps its
         * whole first-stage state — FIR output, IR, the KLib and a `KotlinCoreEnvironment` per module — alive
         * until the grouping stage of its batch is over.
         */
        private const val MAX_TESTS_IN_FLIGHT_PROP = "kotlin.test.grouping.engine.max.tests.in.flight"
        private const val DEFAULT_MAX_TESTS_IN_FLIGHT = 100

        /**
         * How many batches may be processed simultaneously. Most batches are much smaller than
         * [GROUPING_BATCH_SIZE_PROP] (an isolated test forms a batch of its own), and the grouping stage of a
         * single batch is essentially single-threaded, so running only a couple of batches at a time may leave
         * most worker threads idle. Batches are admitted by weight, so a full batch still counts as
         * [GROUPING_BATCH_SIZE_PROP] tests against [MAX_TESTS_IN_FLIGHT_PROP].
         *
         * The default is deliberately conservative, as a grouping stage is the memory-heaviest part of a run and
         * the suites using this engine differ a lot in how much heap they are given and in how many tests they
         * actually group. A suite which has measured a higher value to be both faster and safe should request it
         * explicitly, the way `:wasm:wasm.tests:test` does.
         */
        private const val SIMULTANEOUS_BATCHES_PROP = "kotlin.test.grouping.engine.simultaneous.batches"
        private const val DEFAULT_SIMULTANEOUS_BATCHES = 2

        // Temporary diagnostic switch: turn to `false` to keep a failed grouped batch failed
        // instead of retrying every test in isolation which hides the exceptions during grouping run
        private const val RETRY_FAILED_GROUPS_IN_ISOLATION = true
    }

    override fun getId(): String = ID

    private class ExecutionContext(params: ConfigurationParameters) : Closeable {
        private val workerPool: ExecutorService
        val dispatcher: ExecutorCoroutineDispatcher
        val parallelism: Int = DynamicWithMaxThresholdParallelExecutionConfigurationStrategy.computeParallelism(
            params,
            GROUPING_ENGINE_POOL_SIZE_PROP,
            DynamicWithMaxThresholdParallelExecutionConfigurationStrategy.FIXED_THRESHOLD_PROP
        )
        val batchSize: Int
        val maxTestsInFlight: Int
        val simultaneousBatches: Int

        /**
         * The weight every batch costs against [maxTestsInFlight], even a single-test one, so that no more than
         * [SIMULTANEOUS_BATCHES_PROP] batches are ever in flight.
         *
         * Rounded up (`ceil(maxTestsInFlight / simultaneousBatches)`), not down: with a floor, `simultaneousBatches`
         * many minimum-weight batches can undershoot [maxTestsInFlight] enough to let one extra batch sneak in
         * alongside them - e.g. `maxTestsInFlight = 3, simultaneousBatches = 2` floors to a weight of 1, so three
         * singleton batches (weight 1 each) fit in the budget of 3 at once. Rounding up instead guarantees
         * `(simultaneousBatches + 1) * minBatchWeight > maxTestsInFlight`, so the limiter always blocks the
         * `(simultaneousBatches + 1)`-th minimum-weight batch; the trade-off is that when [maxTestsInFlight] isn't
         * evenly divisible by [simultaneousBatches], the last one of the intended [simultaneousBatches] many
         * minimum-weight batches may itself have to wait rather than run concurrently with the rest. That
         * under-admission only costs a little parallelism in an edge case; over-admission would defeat the whole
         * point of this weight - keeping too many memory-heavy grouping stages from ever running at once.
         */
        val minBatchWeight: Int

        init {
            workerPool = Executors.newFixedThreadPool(parallelism)
            dispatcher = workerPool.asCoroutineDispatcher()

            batchSize = params.positiveIntOrNull(GROUPING_BATCH_SIZE_PROP) ?: DEFAULT_GROUPING_BATCH_SIZE
            maxTestsInFlight = params.positiveIntOrNull(MAX_TESTS_IN_FLIGHT_PROP) ?: DEFAULT_MAX_TESTS_IN_FLIGHT
            simultaneousBatches = params.positiveIntOrNull(SIMULTANEOUS_BATCHES_PROP) ?: DEFAULT_SIMULTANEOUS_BATCHES
            // Ceiling division without overflow risk: these are all small, config-sized ints, not attacker-controlled.
            minBatchWeight = (maxTestsInFlight + simultaneousBatches - 1) / simultaneousBatches

            // A batch keeps every one of its tests' pipelines alive - undisposed - until its grouping stage
            // completes, so a batch bigger than the whole in-flight budget could never respect that budget no
            // matter how its weight is computed. `batchWeight` below relies on this to never need to silently
            // under-report a large batch's true weight just to fit `TestsInFlightLimiter.acquire`'s `1..maxWeight`
            // range - failing loudly here beats quietly letting more tests stay in flight than configured.
            require(batchSize <= maxTestsInFlight) {
                "'$GROUPING_BATCH_SIZE_PROP' ($batchSize) must not exceed '$MAX_TESTS_IN_FLIGHT_PROP' ($maxTestsInFlight): " +
                        "a single batch keeps every one of its tests' pipelines alive until its grouping stage " +
                        "completes, so a batch larger than the in-flight budget could never respect it."
            }
        }

        fun batchWeight(batchSize: Int): Int = maxOf(batchSize, minBatchWeight)

        override fun close() {
            workerPool.shutdown()
            workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

    override fun execute(request: ExecutionRequest) {
        ExecutionContext(request.configurationParameters).use { ctx ->
            val synchronizedListener = SynchronizedEngineExecutionListener(request.engineExecutionListener)
            val baseContext = JupiterEngineExecutionContext(
                synchronizedListener,
                getJupiterConfiguration(request),
                LauncherStoreFacade(request.store),
            )

            val containers = mutableListOf<ContainerNode>()
            val methods = mutableListOf<PlannedMethod>()
            startContainers(request.rootTestDescriptor, baseContext, parent = null, containers, methods)
            // Containers which have no test methods anywhere in their subtree have nothing to wait for.
            // `containers` is in top-down order, so finishing it in reverse propagates completions bottom-up.
            containers.asReversed().forEach { it.finishIfNothingPending() }

            runBlocking(ctx.dispatcher) {
                context(ctx) {
                    val batches = planBatches(methods)
                    executeBatches(batches)
                }
            }
        }
    }

    /**
     * Prepares every container of the tree and reports it as started, top-down, collecting all test methods
     * on the way. Nothing is compiled here, so this is a cheap synchronous walk.
     */
    private fun startContainers(
        descriptor: TestDescriptor,
        baseContext: JupiterEngineExecutionContext,
        parent: ContainerNode?,
        containers: MutableList<ContainerNode>,
        methods: MutableList<PlannedMethod>,
    ) {
        if (!descriptor.isContainer) return
        @Suppress("UNCHECKED_CAST")
        val context = (descriptor as? Node<JupiterEngineExecutionContext>)?.prepare(baseContext) ?: baseContext
        val needReport = descriptor !is JupiterEngineDescriptor
        val node = ContainerNode(descriptor, context, parent, needReport)
        parent?.registerPending()
        containers += node
        if (needReport) context.executionListener.executionStarted(descriptor)

        for (child in descriptor.children) {
            when {
                child is TestMethodTestDescriptor -> {
                    node.registerPending()
                    methods += PlannedMethod(node, child)
                }
                child.isContainer -> startContainers(child, context, node, containers, methods)
            }
        }
    }

    context(ctx: ExecutionContext)
    private suspend fun CoroutineScope.planBatches(methods: List<PlannedMethod>): List<List<TestPlanEntry>> {
        val nextMethod = AtomicInteger(0)
        val entries = List(ctx.parallelism) {
            async {
                buildList {
                    while (true) {
                        val index = nextMethod.getAndIncrement()
                        if (index >= methods.size) break
                        val method = methods[index]
                        planTest(method.container, method.descriptor)?.let { add(it) }
                    }
                }
            }
        }.awaitAll().flatten()

        val batches = buildList {
            // Sorting keeps batch composition independent of the order in which the planning pass happened to finish.
            val groupedByKey = entries.groupBy { it.key }.toList().sortedBy { [key, _] -> key.toString() }
            for ([key, keyEntries] in groupedByKey) {
                val sortedEntries = keyEntries.sortedBy { it.descriptor.uniqueId.toString() }
                if (BatchToken.Isolated in key.tokens) {
                    sortedEntries.forEach { add(listOf(it)) }
                } else {
                    sortedEntries.chunked(ctx.batchSize).forEach { add(it) }
                }
            }
        }
        val outermostClassNames = methods.asSequence()
            .mapNotNull { it.container.outermostClassName }
            .distinct()
            .sorted()
            .joinToString(", ")
            .ifEmpty { "<unknown>" }
        logger.info {
            "Grouping engine for [$outermostClassNames]: ${entries.size} tests planned into ${batches.size} batches " +
                    "(max batch size ${ctx.batchSize}, ${batches.count { it.size == 1 }} single-test batches; " +
                    "simultaneous batches ${ctx.simultaneousBatches}, max tests in flight ${ctx.maxTestsInFlight}, " +
                    "worker threads ${ctx.parallelism})"
        }
        return batches
    }

    /**
     * Computes the batch key of a single test without running any compilation, and disposes everything the test
     * has allocated. Tests which are skipped, fail while being configured, or do not participate in batching at
     * all (utility tests without [TestMetadata]) are reported here and excluded from the plan.
     *
     * The whole body below runs under a single `try`/`catch (Throwable)` (see the `catch` block for why): unlike
     * the other stages, nothing here goes through a `runXxxSafely` wrapper at the call site, because [planTest] is
     * invoked directly inside the `buildList { while (true) { ... } }` loop of [planBatches]' planning coroutines,
     * where an uncaught throwable would abort that coroutine's `async` and, via `awaitAll()`, cancel every other
     * planning coroutine with it - silently dropping the rest of the suite's tests from the report instead of
     * just this one.
     */
    private fun planTest(container: ContainerNode, method: TestMethodTestDescriptor): TestPlanEntry? {
        val methodContext = method.prepare(container.context)
        var started = false
        try {
            // Honor JUnit `ExecutionCondition` extensions (e.g. the test-federation smoke-test filter,
            // `SmokeTestExecutionCondition`) before running anything. The standard Jupiter engine evaluates
            // `shouldBeSkipped` for every test as part of its node lifecycle, but this custom engine drives
            // method execution directly, so we have to replicate that check here. Without it, conditionally
            // disabled tests (e.g. tests muted in smoke mode) would always run.
            val skipResult = method.shouldBeSkipped(methodContext)
            if (skipResult.isSkipped) {
                methodContext.executionListener.executionSkipped(method, skipResult.reason.orElse("<unknown reason>"))
                container.pendingFinished()
                return null
            }

            // If there is no `@TestMetadata` annotation, then this is some utility test (like `testAllFilesPresentIn`)
            // and so it should be excluded from grouping processing. Such a test is self-contained, so it is executed
            // right here instead of being planned into a batch.
            if (method.testMethod.annotations.none { it is TestMetadata }) {
                methodContext.executionListener.executionStarted(method)
                started = true
                methodContext.throwableCollector.execute { method.execute(methodContext, DynamicTestExecutorStub) }
                methodContext.reportFinished(method, container)
                return null
            }

            // Executing the test method only initializes the test runners and extracts the module structure,
            // which is enough for the isolators to compute the batch key. The compilation stages are driven
            // by the engine itself and are not run until the batch of this test is executed.
            var key: BatchKey? = null
            methodContext.throwableCollector.execute {
                method.execute(methodContext, DynamicTestExecutorStub)
                key = methodContext.twoStageTestInstance.computeBatchKey()
            }
            methodContext.twoStageTestInstanceOrNull?.let { testInstance ->
                methodContext.throwableCollector.execute { testInstance.disposeStageRunners() }
            }

            val computedKey = key
            if (computedKey == null) {
                if (methodContext.throwableCollector.isEmpty) {
                    methodContext.throwableCollector.execute {
                        error("Failed to compute a grouping batch key for '${method.displayName}'")
                    }
                }
                methodContext.executionListener.executionStarted(method)
                methodContext.reportFinished(method, container)
                return null
            }
            if (methodContext.throwableCollector.isNotEmpty) {
                methodContext.executionListener.executionStarted(method)
                methodContext.reportFinished(method, container)
                return null
            }
            return TestPlanEntry(container, method, computedKey)
        } catch (e: Throwable) {
            // `ThrowableCollector.execute` above deliberately rethrows unrecoverable throwables such as
            // `OutOfMemoryError` instead of collecting them (see its uses elsewhere in this file), so reaching
            // here means one escaped - most plausibly from `method.execute`, which is where the actual first-stage
            // compilation work (and therefore most of the memory pressure, e.g. the Native OOM case) happens.
            // Without this catch that throwable would propagate out of `planTest` uncaught: see the kdoc above for
            // why that is worse than reporting a single failed test. This mirrors the safety net `executeBatch` and
            // `runNonGroupingStageSafely` already apply to the later stages - `planTest` was the one stage missing
            // it. Best-effort dispose whatever this test allocated before failing, without letting a second
            // failure during disposal hide the original one.
            try {
                methodContext.twoStageTestInstanceOrNull?.disposeStageRunners()
            } catch (disposeFailure: Throwable) {
                e.addSuppressed(disposeFailure)
            }
            if (!started) methodContext.executionListener.executionStarted(method)
            methodContext.executionListener.executionFinished(method, TestExecutionResult.failed(e))
            container.pendingFinished()
            return null
        }
    }

    context(ctx: ExecutionContext)
    private suspend fun CoroutineScope.executeBatches(batches: List<List<TestPlanEntry>>) {
        val limiter = TestsInFlightLimiter(ctx.maxTestsInFlight)
        batches.mapIndexed { index, batch ->
            launch {
                val weight = ctx.batchWeight(batch.size)
                limiter.acquire(weight)
                try {
                    executeBatch(batch, index = index + 1)
                } finally {
                    limiter.release(weight)
                }
            }
        }.joinAll()
    }

    private suspend fun executeBatch(batch: List<TestPlanEntry>, index: Int) {
        val infos = coroutineScope {
            batch.map { entry -> async { runNonGroupingStageSafely(entry, reportStarted = true) } }.awaitAll()
        }
        // Tests whose non-grouping stage failed are already reported; the rest still need the grouping stage.
        val pending = batch.zip(infos).mapNotNull { [entry, info] -> info?.let { entry to it } }
        if (pending.isEmpty()) return
        val pendingInfos = pending.map { it.second }

        val ranAsGroup = try {
            if (pendingInfos.size == 1) {
                runGroupingStageOnSingleSizedBatch(pendingInfos.single())
                true
            } else {
                val containerNode = closestCommonAncestor(pendingInfos.map { it.container })
                runGroupingStageOnBatch(
                    containerNode.context,
                    containerNode.descriptor,
                    pendingInfos,
                    index = index,
                )
            }
        } catch (e: Throwable) {
            // A failure of the grouping stage must not tear the whole engine down, otherwise all the tests
            // which have not been executed yet would silently disappear from the test report. This is a
            // last-resort safety net: both callees above are expected to handle their own failures - including
            // unrecoverable throwables such as `OutOfMemoryError` - internally without throwing.
            pendingInfos.forEach {
                it.finalizeNonGroupingStage()
                it.finalizeGroupingStage()
                it.reportFinished(TestExecutionResult.failed(e))
            }
            true
        }

        if (ranAsGroup) {
            pendingInfos.forEach { it.accountFinished() }
        } else {
            retryBatchInIsolation(pending.map { it.first }, index)
        }
    }

    /**
     * Re-runs the tests of a batch which could not be compiled or executed as a group, one by one in isolated
     * batches. Grouping several tests into one compilation is an optimization, and there are tests which cannot
     * be grouped — the isolators only recognize the known cases. Without this fallback a single such test would
     * fail every other test which happened to share its batch, which is both a wrong result for up to
     * `batch.size - 1` tests and a misleading one for the test that is actually at fault.
     */
    private fun retryBatchInIsolation(entries: List<TestPlanEntry>, index: Int) {
        println(
            "Grouping engine: grouped batch #$index of ${entries.size} tests could not be run as a group, " +
                    "re-running its tests in isolated batches"
        )
        // Sequentially: the batch holds permits for `entries.size` tests, but every retried test now needs a
        // grouping stage of its own, which is far heavier than the first-stage state those permits account for.
        for (entry in entries) {
            // The tests have already been reported as started by the failed attempt.
            val info = runNonGroupingStageSafely(entry, reportStarted = false) ?: continue
            try {
                // `runGroupingStageOnSingleSizedBatch` handles its own failures, including unrecoverable
                // throwables, internally; this is a last-resort safety net.
                runGroupingStageOnSingleSizedBatch(info)
            } catch (e: Throwable) {
                info.finalizeNonGroupingStage()
                info.finalizeGroupingStage()
                info.reportFinished(TestExecutionResult.failed(e))
            } finally {
                info.accountFinished()
            }
        }
    }

    private fun runNonGroupingStageSafely(entry: TestPlanEntry, reportStarted: Boolean): TestMethodInfo? {
        var started = !reportStarted
        return try {
            runNonGroupingStage(entry, reportStarted) { started = true }
        } catch (e: Throwable) {
            // Same reasoning as in `executeBatch`: an escaping throwable must not take down the whole engine.
            // This only guards `method.prepare`/`shouldBeSkipped` above, since `runNonGroupingStage` itself never
            // lets a throwable escape (see the comment there).
            val listener = entry.container.context.executionListener
            if (!started) listener.executionStarted(entry.descriptor)
            listener.executionFinished(entry.descriptor, TestExecutionResult.failed(e))
            entry.container.pendingFinished()
            null
        }
    }

    /**
     * @param reportStarted `false` when the test has already been reported as started by a grouped attempt which
     *   failed and is now being retried in isolation, see [retryBatchInIsolation].
     * @return the test to be executed by the grouping stage, or `null` if the test is already fully reported.
     */
    private fun runNonGroupingStage(entry: TestPlanEntry, reportStarted: Boolean, onStarted: () -> Unit): TestMethodInfo? {
        val method = entry.descriptor
        val methodContext = method.prepare(entry.container.context)

        val skipResult = method.shouldBeSkipped(methodContext)
        if (skipResult.isSkipped) {
            methodContext.executionListener.executionSkipped(method, skipResult.reason.orElse("<unknown reason>"))
            entry.container.pendingFinished()
            return null
        }

        if (reportStarted) methodContext.executionListener.executionStarted(method)
        onStarted()

        // `ThrowableCollector.execute` below deliberately rethrows unrecoverable throwables such as
        // `OutOfMemoryError` instead of collecting them, bypassing `finishIfFailed()` and leaving `info` (once
        // created) undisposed and unreported. Catch here, where both are still in scope, instead of letting such
        // a throwable escape to a caller which no longer has access to either.
        var info: TestMethodInfo? = null
        try {
            methodContext.throwableCollector.execute { method.execute(methodContext, DynamicTestExecutorStub) }
            info = TestMethodInfo(method, methodContext, methodContext.twoStageTestInstance, entry.container)
            if (info.finishIfFailed()) return null

            info.nonGroupingStageThrowableCollector.execute {
                val testRunner = info.testInstance.nonGroupingRunner
                testRunner.runTestPreprocessing()
                testRunner.runSteps()
                info.hadIgnoredFailuresOnNonGroupingStage = testRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            }
            if (info.finishIfFailed()) return null
            return info
        } catch (e: Throwable) {
            val current = info
            if (current != null) {
                current.finalizeNonGroupingStage()
                current.finalizeGroupingStage()
                current.reportFinished(TestExecutionResult.failed(e))
            } else {
                methodContext.executionListener.executionFinished(method, TestExecutionResult.failed(e))
                entry.container.pendingFinished()
            }
            return null
        }
    }

    /**
     * @param containerDescriptor the closest common ancestor container of every test in [batch] (see
     *   [closestCommonAncestor]), so the synthetic batch node added below is guaranteed to be an ancestor of -
     *   i.e. sit in the same folder or an enclosing one relative to - every test it represents, rather than a
     *   sibling of only some of them.
     * @return `true` if the batch was compiled and executed as a group, in which case all its tests are reported.
     *   `false` if the grouping stage itself failed, which says nothing about the individual tests: none of them
     *   is reported and they have to be retried in isolation, see [retryBatchInIsolation].
     */
    private fun runGroupingStageOnBatch(
        context: JupiterEngineExecutionContext,
        containerDescriptor: TestDescriptor,
        batch: List<TestMethodInfo>,
        index: Int,
    ): Boolean {
        require(batch.size > 1) { "Batch expected to have at least 2 methods, got ${batch.size}" }
        val testDescriptor = GroupingStageTestDescriptor(
            uniqueId = containerDescriptor.uniqueId.append("dynamic-test", "batch$index"),
            displayName = "Grouped batch #$index"
        )
        containerDescriptor.addChild(testDescriptor)
        val throwableCollector = createThrowableCollector()
        val someTestInstance = batch.first().testInstance

        val executionListener = context.executionListener
        executionListener.dynamicTestRegistered(testDescriptor)
        executionListener.executionStarted(testDescriptor)

        // Abort the batch (dispose its tests and, normally, let the caller retry them in isolation) the same way
        // whether the failure was collected normally or is a throwable which escaped `ThrowableCollector.execute` below.
        // `ThrowableCollector.execute` deliberately rethrows unrecoverable throwables such as `OutOfMemoryError`
        // instead of collecting them, so without this the batch descriptor would never be finished and its tests
        // would never be disposed or retried.
        fun abortBatch(result: TestExecutionResult, failure: Throwable? = null): Boolean {
            executionListener.executionFinished(testDescriptor, result)
            batch.forEach {
                it.finalizeNonGroupingStage()
                it.finalizeGroupingStage()
                if (!RETRY_FAILED_GROUPS_IN_ISOLATION) {
                    it.reportFinished(failure?.let { throwable -> TestExecutionResult.failed(throwable) } ?: result)
                }
            }
            return !RETRY_FAILED_GROUPS_IN_ISOLATION
        }

        val escapedThrowable = try {
            throwableCollector.execute {
                val testRunner = someTestInstance.groupingStageRunner
                val nonGroupingStageOutputs = batch.map { methodInfo ->
                    NonGroupingStageOutput(
                        testServices = methodInfo.testInstance.nonGroupingRunner.testServices,
                        catchingExecutor = { wrapper, block ->
                            methodInfo.testInstance.nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                        }
                    )
                }
                testRunner.run(nonGroupingStageOutputs)
                testRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
            }
            null
        } catch (e: Throwable) {
            e
        }
        if (escapedThrowable != null) return abortBatch(TestExecutionResult.failed(escapedThrowable), escapedThrowable)

        if (throwableCollector.isNotEmpty) {
            // The batch as a whole did not make it through the grouping stage, so there is no per-test result to
            // report: the failure may just as well come from a single test of the batch which cannot be grouped.
            // Report the synthetic batch descriptor as aborted (it shows up as skipped, with the reason attached)
            // and, unless the temporary diagnostic switch above is disabled, let the caller re-run the tests one
            // by one to find out what really failed.
            return abortBatch(TestExecutionResult.aborted(throwableCollector.throwable), throwableCollector.throwable)
        }

        try {
            for (methodInfo in batch) {
                methodInfo.context.throwableCollector.execute {
                    methodInfo.testInstance.nonGroupingRunner.failuresInterceptor.reportFailures(
                        // we need to check for unmuting only if there were no exceptions from the
                        // grouped facades
                        checkForUnmuting = throwableCollector.isEmpty
                    )
                }
            }
        } catch (e: Throwable) {
            return abortBatch(TestExecutionResult.failed(e), e)
        }

        executionListener.executionFinished(testDescriptor, throwableCollector.toTestExecutionResult())
        batch.forEach {
            it.finalizeNonGroupingStage()
            it.finalizeGroupingStage()
            val collector = if (it.failed) it.nonGroupingStageThrowableCollector else throwableCollector
            it.reportFinished(collector)
        }
        return true
    }

    private fun runGroupingStageOnSingleSizedBatch(testInfo: TestMethodInfo) {
        val throwableCollector = testInfo.nonGroupingStageThrowableCollector
        val testInstance = testInfo.testInstance
        // `ThrowableCollector.execute` deliberately rethrows unrecoverable throwables such as `OutOfMemoryError`
        // instead of collecting them into `throwableCollector`, which would otherwise stay empty and make the
        // reporting below claim success. Catch it here and report it directly instead.
        val escapedThrowable = try {
            throwableCollector.execute {
                val groupingRunner = testInstance.groupingStageRunner
                val nonGroupingRunner = testInstance.nonGroupingRunner
                val nonGroupingStageOutput = NonGroupingStageOutput(
                    testServices = testInstance.nonGroupingRunner.testServices,
                    catchingExecutor = { wrapper, block ->
                        nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                    }
                )
                groupingRunner.run(listOf(nonGroupingStageOutput))

                /*
                 * Exceptions from facades were reported to the failures interceptor of the grouping runner.
                 * However, failure suppressors should be run from non-grouping runner, as they need access to
                 * the real module structure of the specific test to be able to extract directives from there.
                 */
                nonGroupingRunner.failuresInterceptor += groupingRunner.failuresInterceptor
                nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
            }
            null
        } catch (e: Throwable) {
            e
        }

        testInfo.finalizeNonGroupingStage()
        testInfo.finalizeGroupingStage()
        if (escapedThrowable != null) {
            testInfo.reportFinished(TestExecutionResult.failed(escapedThrowable))
        } else {
            testInfo.reportFinished(throwableCollector)
        }
    }

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val configuration = CachingJupiterConfiguration(
            DefaultJupiterConfiguration(
                discoveryRequest.configurationParameters,
                discoveryRequest.outputDirectoryCreator
            )
        )
        val engineDescriptor = JupiterEngineDescriptor(uniqueId, configuration)
        DiscoverySelectorResolver().resolveSelectors(discoveryRequest, engineDescriptor)

        filterDescriptor(engineDescriptor)

        return engineDescriptor
    }

    private fun filterDescriptor(descriptor: TestDescriptor) {
        val childrenToRemove = descriptor.children.filter { child ->
            !shouldIncludeDescriptor(child)
        }
        childrenToRemove.forEach { descriptor.removeChild(it) }
    }

    private fun shouldIncludeDescriptor(descriptor: TestDescriptor): Boolean {
        if (descriptor is ClassTestDescriptor) {
            val testClass = descriptor.testClass
            if (testClass != null) {
                return testClass.isTwoStageKotlinCompilerTest()
            }
        }

        // For test methods, check the parent class
        if (descriptor is TestMethodTestDescriptor) {
            return descriptor.testClass.isTwoStageKotlinCompilerTest()
        }

        return true
    }

    private fun getJupiterConfiguration(request: ExecutionRequest): JupiterConfiguration {
        val engineDescriptor = request.rootTestDescriptor as JupiterEngineDescriptor
        return engineDescriptor.configuration
    }
}

private fun ConfigurationParameters.positiveIntOrNull(name: String): Int? =
    get(name).orElse(null)?.toInt()?.also { require(it > 0) { "'$name' must be positive, but was $it" } }

private val JupiterEngineExecutionContext.twoStageTestInstanceOrNull: AbstractTwoStageKotlinCompilerTestBase?
    get() = extensionContext.requiredTestInstances
        .findInstance(AbstractTwoStageKotlinCompilerTestBase::class.java)
        .orElse(null)

private val JupiterEngineExecutionContext.twoStageTestInstance: AbstractTwoStageKotlinCompilerTestBase
    get() = twoStageTestInstanceOrNull
        ?: error("${AbstractTwoStageKotlinCompilerTestBase::class.simpleName} instance is expected")

private fun JupiterEngineExecutionContext.reportFinished(descriptor: TestDescriptor, container: ContainerNode) {
    executionListener.executionFinished(descriptor, throwableCollector.toTestExecutionResult())
    container.pendingFinished()
}

private fun AbstractTwoStageKotlinCompilerTestBase.disposeStageRunners() {
    if (nonGroupingStageRunnerInitialized) nonGroupingRunner.finalizeAndDispose()
    if (secondStageRunnerInitialized) groupingStageRunner.finalizeAndDispose()
}

private fun AbstractTwoStageKotlinCompilerTestBase.computeBatchKey(): BatchKey {
    val testConfiguration = nonGroupingRunner.testConfiguration
    val tokens = testConfiguration.groupingTestIsolators.mapNotNull {
        it.computeBatchToken(testConfiguration.testServices.moduleStructure).takeIf { token -> token != BatchToken.Regular }
    }
    return BatchKey(groupingBatchScope, tokens)
}

/**
 * [scope] is [AbstractTwoStageKotlinCompilerTestBase.groupingBatchScope], which defaults to the concrete generated
 * JUnit test class (e.g. `WasmJsCodegenBoxTestGenerated`) and is always part of the key, in addition to whatever
 * [tokens] the suite's [org.jetbrains.kotlin.test.model.GroupingTestIsolator]s compute.
 *
 * A test class configures the runner used for its grouping stage - target platform, backend facade, output
 * directory layout, and so on - through its own `configure()` override, and the engine has no generic way to
 * compare two such configurations for compatibility. Two tests from different test classes can therefore end up
 * with the exact same isolator tokens (typically both `Regular`, e.g. two tests sharing identical directives but
 * targeting different platforms) while still being incompatible to compile together: defaulting [scope] to the
 * runtime class guarantees they are still never placed in the same batch, without requiring every isolator to
 * reimplement this check itself. For example, `WasmJsCodegenBoxTestGenerated` and `WasmWasiCodegenBoxTestGenerated`
 * share `WasmGroupingTestIsolator`, which does not encode the Wasm target (JS vs WASI) in its tokens; without this
 * default, identical-looking box tests for both targets could be batched together, and the grouping stage would
 * compile the whole batch using only the first test's target, failing the rest with a KLib platform mismatch
 * (`Expected target is WASM[wasm-js] while found WASM[wasm-wasi]`).
 *
 * A family of generated classes whose `configure()` is verified to be identical top to bottom - same grouping-stage
 * facade and handlers, same non-grouping-stage transformers/configurators, same target - may opt out of this
 * per-class fragmentation by overriding [AbstractTwoStageKotlinCompilerTestBase.groupingBatchScope] to a value they
 * all share, so their tests can be pooled into the same batches instead of each class filling out its own tail
 * batch independently. This is an explicit, human-verified opt-in rather than something the engine infers, because
 * - as the KLib platform mismatch above shows - the engine cannot check compatibility on the suite's behalf.
 */
private data class BatchKey(val scope: Any, val tokens: List<BatchToken>)

/**
 * Admits batches so that the total weight of the batches in flight never exceeds [maxWeight].
 *
 * A plain [kotlinx.coroutines.sync.Semaphore] cannot be used, as it only hands out one permit at a time:
 * acquiring N permits in a loop deadlocks as soon as two batches each hold a part of what the other one waits for.
 * Waiters are admitted strictly in order instead, so a heavy batch is never starved by a stream of light ones.
 */
private class TestsInFlightLimiter(private val maxWeight: Int) {
    private val mutex = Mutex()
    private var weightInFlight = 0
    private val waiters = ArrayDeque<Pair<Int, CompletableDeferred<Unit>>>()

    suspend fun acquire(weight: Int) {
        require(weight in 1..maxWeight) { "Weight $weight is out of the 1..$maxWeight range" }
        val waiter = mutex.withLock {
            if (waiters.isEmpty() && weightInFlight + weight <= maxWeight) {
                weightInFlight += weight
                null
            } else {
                CompletableDeferred<Unit>().also { waiters.addLast(weight to it) }
            }
        }
        waiter?.await()
    }

    suspend fun release(weight: Int) {
        val admitted = mutex.withLock {
            weightInFlight -= weight
            buildList {
                while (true) {
                    val [waiterWeight, waiter] = waiters.firstOrNull() ?: break
                    if (weightInFlight + waiterWeight > maxWeight) break
                    waiters.removeFirst()
                    weightInFlight += waiterWeight
                    add(waiter)
                }
            }
        }
        admitted.forEach { it.complete(Unit) }
    }
}

private class PlannedMethod(val container: ContainerNode, val descriptor: TestMethodTestDescriptor)

private class TestPlanEntry(
    val container: ContainerNode,
    val descriptor: TestMethodTestDescriptor,
    val key: BatchKey,
)

/**
 * A container (test class or a nested test class) which has been reported as started and is waiting for its
 * test methods and nested containers to finish. Because batches are formed across the whole test suite, methods
 * of one class are spread over many batches, so a container is finished as soon as the last of the things it is
 * waiting for is done, rather than when some particular batch completes.
 */
private class ContainerNode(
    val descriptor: TestDescriptor,
    val context: JupiterEngineExecutionContext,
    val parent: ContainerNode?,
    private val needReport: Boolean,
) {
    private val pending = AtomicInteger(0)
    private val finished = AtomicBoolean(false)

    fun registerPending() {
        pending.incrementAndGet()
    }

    fun pendingFinished() {
        if (pending.decrementAndGet() == 0) finish()
    }

    fun finishIfNothingPending() {
        if (pending.get() == 0) finish()
    }

    private fun finish() {
        if (!finished.compareAndSet(false, true)) return
        if (needReport) {
            context.executionListener.executionFinished(descriptor, context.throwableCollector.toTestExecutionResult())
        }
        parent?.pendingFinished()
    }
}

private val ContainerNode.outermostClassName: String?
    get() = generateSequence(this) { it.parent }
        .map { it.descriptor }
        .filterIsInstance<ClassTestDescriptor>()
        .lastOrNull()
        ?.testClass
        ?.simpleName

/**
 * The closest container whose subtree contains every one of [nodes], found by walking each node's ancestor
 * chain up to the tree root and picking the deepest node common to all of them.
 *
 * All tests of one batch necessarily share the class-level container that [BatchKey.testClass] identifies (see
 * its kdoc), so that container is always a valid fallback common ancestor; this may still resolve to something
 * deeper - e.g. a `@Nested` container mirroring a testData sub-folder - when every test of the batch happens to
 * live under it.
 */
private fun closestCommonAncestor(nodes: List<ContainerNode>): ContainerNode =
    nodes.reduce { a, b -> closestCommonAncestor(a, b) }

private fun closestCommonAncestor(a: ContainerNode, b: ContainerNode): ContainerNode {
    val ancestorsOfA = generateSequence(a) { it.parent }.toHashSet()
    return generateSequence(b) { it.parent }.first { it in ancestorsOfA }
}

/**
 * Thread-safe wrapper around EngineExecutionListener.
 * Gradle's execution listener is not thread-safe, so all calls must be synchronized.
 */
private class SynchronizedEngineExecutionListener(
    private val delegate: EngineExecutionListener,
) : EngineExecutionListener {
    override fun dynamicTestRegistered(testDescriptor: TestDescriptor?) = synchronized(this) {
        delegate.dynamicTestRegistered(testDescriptor)
    }

    override fun executionStarted(testDescriptor: TestDescriptor?) = synchronized(this) {
        delegate.executionStarted(testDescriptor)
    }

    override fun executionSkipped(testDescriptor: TestDescriptor?, reason: String?) = synchronized(this) {
        delegate.executionSkipped(testDescriptor, reason)
    }

    override fun executionFinished(testDescriptor: TestDescriptor?, testExecutionResult: TestExecutionResult?) = synchronized(this) {
        delegate.executionFinished(testDescriptor, testExecutionResult)
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor?, entry: ReportEntry?) = synchronized(this) {
        delegate.reportingEntryPublished(testDescriptor, entry)
    }
}

private object DynamicTestExecutorStub : Node.DynamicTestExecutor {
    override fun execute(testDescriptor: TestDescriptor) {
        shouldNotBeCalled()
    }

    override fun execute(
        testDescriptor: TestDescriptor,
        executionListener: EngineExecutionListener,
    ): Future<*> {
        shouldNotBeCalled()
    }

    override fun awaitFinished() {
        shouldNotBeCalled()
    }
}

private class GroupingStageTestDescriptor(
    uniqueId: UniqueId,
    displayName: String,
) : AbstractTestDescriptor(uniqueId, displayName, /* source = */ null) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
}

private class TestMethodInfo(
    val descriptor: TestMethodTestDescriptor,
    val context: JupiterEngineExecutionContext,
    val testInstance: AbstractTwoStageKotlinCompilerTestBase,
    val container: ContainerNode,
) {
    val failed: Boolean
        get() = nonGroupingStageThrowableCollector.isNotEmpty

    var hadIgnoredFailuresOnNonGroupingStage: Boolean = false

    private var finalized: Boolean = false
    private val reported = AtomicBoolean(false)
    private val accounted = AtomicBoolean(false)

    val nonGroupingStageThrowableCollector: ThrowableCollector
        get() = context.throwableCollector

    fun finalizeNonGroupingStage() {
        if (finalized) return
        nonGroupingStageThrowableCollector.execute {
            if (testInstance.nonGroupingStageRunnerInitialized) {
                testInstance.nonGroupingRunner.finalizeAndDispose()
            }
        }
        finalized = true
    }

    fun finalizeGroupingStage() {
        if (!testInstance.secondStageRunnerInitialized) return
        nonGroupingStageThrowableCollector.execute {
            testInstance.groupingStageRunner.finalizeAndDispose()
        }
    }

    fun reportFinished(throwableCollector: ThrowableCollector) {
        reportFinished(throwableCollector.toTestExecutionResult())
    }

    /**
     * Reports the test as finished with an already-computed [result], instead of a [ThrowableCollector]. Use this
     * for a [Throwable] caught directly by our own code (wrapped in [TestExecutionResult.failed]) rather than
     * routing it through [ThrowableCollector.execute] a second time: that method deliberately rethrows
     * unrecoverable throwables such as `OutOfMemoryError` instead of collecting them, so re-executing an
     * already-caught one would just throw again instead of ever producing a result.
     */
    fun reportFinished(result: TestExecutionResult) {
        if (!reported.compareAndSet(false, true)) return
        context.executionListener.executionFinished(descriptor, result)
        accountFinished()
    }

    fun accountFinished() {
        if (accounted.compareAndSet(false, true)) container.pendingFinished()
    }
}

/**
 * @returns true if the test failed
 */
private fun TestMethodInfo.finishIfFailed(): Boolean {
    return (failed || hadIgnoredFailuresOnNonGroupingStage).also {
        if (it) {
            finalizeNonGroupingStage()
            finalizeGroupingStage()
            reportFinished(nonGroupingStageThrowableCollector)
        }
    }
}
