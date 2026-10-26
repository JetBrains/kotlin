/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.hierarchical.Node
import org.junit.platform.engine.support.hierarchical.ThrowableCollector
import org.opentest4j.TestAbortedException
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Executes two-stage compiler tests ([AbstractTwoStageKotlinCompilerTestBase]), one top-level test class at a time, in two passes.
 *
 * The planning pass executes the test method of every test of the class, which is cheap: it only builds the test
 * configurations and extracts the module structure, which is all the [org.jetbrains.kotlin.test.model.GroupingTestIsolator]s
 * need to compute the batch tokens. Everything the test allocated is disposed right away, so only the tokens are kept.
 *
 * Then the tests of the whole class, including all its `@Nested` classes, which have equal tokens are packed into
 * batches of at most [SIMULTANEOUS_METHODS_GROUP_SIZE_PROP] tests, and the expensive stages run batch by batch: the
 * non-grouping stage for every test of a batch, followed by a single grouping stage for the whole batch.
 * Tests of different top-level classes never share a batch: such classes may configure different targets or runners
 * for the grouping stage, which the engine has no generic way to compare.
 */
class CompilerTestGroupingTestEngine : TestEngine {
    companion object {
        const val ID = "kotlin-compiler-grouping-engine"

        private const val GROUPING_ENGINE_POOL_SIZE_PROP = "kotlin.test.grouping.engine.pool.size"

        /**
         * How many batches may be processed simultaneously. Together with [SIMULTANEOUS_METHODS_GROUP_SIZE_PROP], this bounds
         * the memory consumption of a run, as a test keeps its first-stage state alive until the grouping stage of its batch is over.
         */
        private const val SIMULTANEOUS_METHODS_GROUPS_PROP = "kotlin.test.grouping.engine.simultaneous.methods.groups"
        private const val DEFAULT_SIMULTANEOUS_METHODS_GROUPS = 2

        /**
         * A hard upper bound on the number of tests which are compiled and executed together as a single batch.
         * Grouping-stage memory consumption grows with the batch size, as a batch is linked and lowered as a
         * single IR world containing the standard library and the KLib of every test of the batch.
         */
        private const val SIMULTANEOUS_METHODS_GROUP_SIZE_PROP = "kotlin.test.grouping.engine.simultaneous.methods.group.size"
        private const val DEFAULT_SIMULTANEOUS_METHODS_GROUP_SIZE = 50

        /**
         * Whether the tests which failed in a grouped batch are retried in isolation (see [retryInIsolation]): all tests of
         * a batch whose grouping stage failed as a whole, and every single test which is not isolated by its batch tokens
         * and failed at any stage.
         * Set it to `false` to report such tests as they failed in the batch instead: retries hide the failures which
         * grouping causes, so this is the way to find tests which the isolators fail to separate.
         */
        private const val RETRY_FAILED_TESTS_PROP = "kotlin.test.grouping.engine.retry.failed.tests.isolated"
    }

    override fun getId(): String = ID

    private class ExecutionContext(params: ConfigurationParameters) : Closeable {
        private val workerPool: ExecutorService
        val dispatcher: ExecutorCoroutineDispatcher
        val activeBatches: Semaphore
        val methodsChunkSize: Int
        val retryFailedTests: Boolean = params.getBoolean(RETRY_FAILED_TESTS_PROP).orElse(null) ?: true

        init {
            val parallelism = DynamicWithMaxThresholdParallelExecutionConfigurationStrategy.computeParallelism(
                params,
                GROUPING_ENGINE_POOL_SIZE_PROP,
                DynamicWithMaxThresholdParallelExecutionConfigurationStrategy.FIXED_THRESHOLD_PROP
            )
            workerPool = Executors.newFixedThreadPool(parallelism)
            dispatcher = workerPool.asCoroutineDispatcher()

            val numberOfActiveBatches = params.get(SIMULTANEOUS_METHODS_GROUPS_PROP).orElse(null)?.toInt()?.also {
                require(it > 0) { "Number of simultaneous method groups must be positive, but was $it" }
            } ?: DEFAULT_SIMULTANEOUS_METHODS_GROUPS

            activeBatches = Semaphore(permits = numberOfActiveBatches)

            methodsChunkSize = params.get(SIMULTANEOUS_METHODS_GROUP_SIZE_PROP).orElse(null)?.toInt()?.also {
                require(it > 0) { "Number of methods in a group must be positive, but was $it" }
            } ?: DEFAULT_SIMULTANEOUS_METHODS_GROUP_SIZE
        }

        override fun close() {
            workerPool.shutdown()
            workerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

    override fun execute(request: ExecutionRequest) {
        ExecutionContext(request.configurationParameters).use { ctx ->
            val root = request.rootTestDescriptor
            val synchronizedListener = SynchronizedEngineExecutionListener(request.engineExecutionListener)
            val baseContext = JupiterEngineExecutionContext(
                synchronizedListener,
                getJupiterConfiguration(request),
                LauncherStoreFacade(request.store),
            )

            @Suppress("UNCHECKED_CAST")
            val rootContext = (root as Node<JupiterEngineExecutionContext>).prepare(baseContext)
            runBlocking(ctx.dispatcher) {
                context(ctx) {
                    for (topLevelClass in root.children.filter { it.isContainer }) {
                        launch { processTopLevelClass(topLevelClass, rootContext) }
                    }
                }
            }
        }
    }

    context(ctx: ExecutionContext)
    private suspend fun processTopLevelClass(classDescriptor: TestDescriptor, parentContext: JupiterEngineExecutionContext) {
        val containers = mutableListOf<Pair<TestDescriptor, JupiterEngineExecutionContext>>()
        val methods = mutableListOf<TestMethodWithContext>()
        prepareContainers(classDescriptor, parentContext, containers, methods)
        val classContext = containers.first().second
        try {
            val batches = planBatches(methods)
            coroutineScope {
                batches.forEachIndexed { index, batch ->
                    launch {
                        ctx.activeBatches.withPermit {
                            try {
                                executeBatch(classContext, classDescriptor, batch, index = index + 1)
                            } catch (e: Throwable) {
                                // A last resort: a failure escaping here must not cancel the other batches, which would
                                // silently drop all their tests from the report.
                                batch.tests.forEach { it.context.failIfUnfinished(it.method, e) }
                            }
                        }
                    }
                }
            }
        } finally {
            // The tests of a nested class are spread over the batches of the whole top-level class,
            // so the finalization of containers is performed here, only once all the batches are done.
            // Per javadoc for `EngineExecutionListener.executionFinished()`:
            //   at the moment of container finalization, its non-finished children will be marked as SKIPPED.
            // So for correct finalization, `containers` must be iterated in the reversed order,
            // so nested classes will be finished before their parents
            for ([descriptor, context] in containers.asReversed()) {
                context.executionListener.executionFinished(descriptor, context.throwableCollector.toTestExecutionResult())
            }
        }
    }

    /** Prepares every container of the subtree and reports it as started, top-down, collecting all test methods on the way. */
    private fun prepareContainers(
        descriptor: TestDescriptor,
        parentContext: JupiterEngineExecutionContext,
        containers: MutableList<Pair<TestDescriptor, JupiterEngineExecutionContext>>,
        methods: MutableList<TestMethodWithContext>,
    ) {
        @Suppress("UNCHECKED_CAST")
        val context = (descriptor as? Node<JupiterEngineExecutionContext>)?.prepare(parentContext) ?: parentContext
        context.executionListener.executionStarted(descriptor)
        containers += descriptor to context
        for (child in descriptor.children) {
            when {
                child is TestMethodTestDescriptor -> methods += TestMethodWithContext(child, context)
                child.isContainer -> prepareContainers(child, context, containers, methods)
            }
        }
    }

    /** Packs the tests of [methods] with equal batch tokens into batches. */
    context(ctx: ExecutionContext)
    private suspend fun planBatches(methods: List<TestMethodWithContext>): List<PlannedBatch> {
        val tokens = coroutineScope {
            methods.map {
                async {
                    try {
                        planTokens(it.method, it.context)
                    } catch (e: Throwable) {
                        it.context.failIfUnfinished(it.method, e)
                        null
                    }
                }
            }.awaitAll()
        }
        val plannedMethods = methods.zip(tokens).mapNotNull { [method, tokens] -> tokens?.let { method to it } }
        return plannedMethods.groupBy({ it.second }, { it.first }).flatMap { [tokens, sameTokenMethods] ->
            // Sorted by unique id, so that which tests share a batch does not depend on the order JUnit lists them in.
            val sortedMethods = sameTokenMethods.sortedBy { it.method.uniqueId.toString() }
            if (BatchToken.Isolated in tokens) {
                sortedMethods.map { PlannedBatch(listOf(it), isolated = true) }
            } else {
                sortedMethods.chunked(ctx.methodsChunkSize) { PlannedBatch(it.toList(), isolated = false) }
            }
        }
    }

    /**
     * Computes the batch tokens of a test by executing its test method, which only initializes the test runners and
     * extracts the module structure. Everything the test has allocated is disposed right away.
     * @return `null` if the test is skipped or has failed while being configured, in which case it is already reported.
     */
    private fun planTokens(method: TestMethodTestDescriptor, context: JupiterEngineExecutionContext): List<BatchToken>? {
        // If there is no `@TestMetadata` annotation, then this is some utility test (like `testAllFilesPresentIn`) or a
        // sanity test which drives the test runners itself, so it must not be executed here: it runs alone in its batch.
        if (method.testMethod.annotations.none { it is TestMetadata }) return listOf(BatchToken.Isolated)

        val methodContext = method.prepare(context)
        val skipResult = method.shouldBeSkipped(methodContext)
        if (skipResult.isSkipped) {
            methodContext.executionListener.executionSkipped(method, skipResult.reason.orElse("<unknown reason>"))
            return null
        }
        val collector = methodContext.throwableCollector
        var tokens: List<BatchToken>? = null
        collector.executeCatchingAll { method.execute(methodContext, DynamicTestExecutorStub) }
        val testInstance = methodContext.extensionContext.testInstances
            .flatMap { it.findInstance(AbstractTwoStageKotlinCompilerTestBase::class.java) }
            .orElse(null)
        if (testInstance != null) {
            if (collector.isEmpty) collector.executeCatchingAll { tokens = testInstance.computeBatchTokens() }
            collector.executeCatchingAll { if (testInstance.nonGroupingStageRunnerInitialized) testInstance.nonGroupingRunner.finalizeAndDispose() }
            collector.executeCatchingAll { if (testInstance.secondStageRunnerInitialized) testInstance.groupingStageRunner.finalizeAndDispose() }
        }
        if (collector.isEmpty) return tokens
        // The test has failed while being configured, so it never joins a batch and must be reported right here.
        // No tests have been notified as started during planning stage (it will be started later, during `executeBatch` stage)
        // so to report a failure, executionStarted() must be reported before executionFinished() .
        methodContext.executionListener.executionStarted(method)
        methodContext.executionListener.executionFinished(method, collector.toTestExecutionResult())
        return null
    }

    context(ctx: ExecutionContext)
    private suspend fun executeBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: PlannedBatch,
        index: Int,
    ) {
        // A test which is not isolated by its tokens is prepared for a batch already in its non-grouping stage (e.g.
        // moved to a package of its own), even if it happens to be left alone in its batch, so wherever such a test
        // fails, it is retried in isolation.
        val retryFailures = ctx.retryFailedTests && !batch.isolated
        val failedTests = runStagesOfBatch(context, classDescriptor, batch, index, retryFailures)
        if (failedTests.isNotEmpty()) retryInIsolation(failedTests, batch, index)
    }

    /**
     * Runs the non-grouping stage of every test of the batch, then the grouping stage of the batch.
     * @return the tests to retry in isolation (see [retryInIsolation]), which are disposed, but not reported. Nothing
     *   else of the failed run is returned: it may have run out of memory, so it must not stay reachable during the retries.
     */
    private suspend fun runStagesOfBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: PlannedBatch,
        index: Int,
        retryFailures: Boolean,
    ): List<TestMethodWithContext> {
        val testInfos = coroutineScope {
            batch.tests.map {
                async {
                    try {
                        runNonGroupingStage(it.method, it.context, reportFailure = !retryFailures)
                    } catch (e: Throwable) {
                        // Must not cancel the other tests of the batch, which would neither report nor dispose them.
                        it.context.failIfUnfinished(it.method, e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        val failedTests = testInfos.filter { it.failedWithError && retryFailures }.toMutableList()
        val successfulTests = testInfos.filterNot { it.failed || it.hadIgnoredFailuresOnNonGroupingStage }
        when (successfulTests.size) {
            0 -> {}
            1 -> if (!runGroupingStageOnSingleSizedBatch(successfulTests.single(), reportFailure = !retryFailures)) {
                failedTests += successfulTests.single()
            }
            else -> failedTests += runGroupingStageOnBatch(context, classDescriptor, successfulTests, index, retryFailures)
        }
        return batch.tests.filter { method -> failedTests.any { it.descriptor == method.method } }
    }

    /**
     * Re-runs the tests which failed in a grouped batch, one by one. Grouping is an optimization, and the isolators only
     * recognize the known cases of tests which cannot be grouped: without this fallback a test which the isolators have
     * missed would fail, and if it breaks the grouping stage as a whole, so would every other test of its batch.
     * A retried test is isolated as if an isolator had recognized it (see [RetriedTestIsolator]), and a test which
     * fails on its own merely fails once more.
     * @param failedTests the failed tests, which are disposed, but not reported.
     */
    private fun retryInIsolation(failedTests: List<TestMethodWithContext>, batch: PlannedBatch, index: Int) {
        println(
            "Grouping engine: ${failedTests.size} of ${batch.tests.size} tests of grouped batch #$index failed, re-running them in isolated batches: " +
                    failedTests.joinToString { "${it.method.testClass.name.substringAfterLast('.')}.${it.method.testMethod.name}" }
        )
        // Sequentially: the batch holds a single permit, but every retried test needs a grouping stage of its own.
        for (test in failedTests) {
            try {
                val testInfo = runNonGroupingStage(test.method, test.context, isRetry = true) ?: continue
                if (!testInfo.failed && !testInfo.hadIgnoredFailuresOnNonGroupingStage) {
                    runGroupingStageOnSingleSizedBatch(testInfo, reportFailure = true)
                }
            } catch (e: Throwable) {
                // Must not fail the tests still waiting for their retry.
                test.context.failIfUnfinished(test.method, e)
            }
        }
    }

    /**
     * @param isRetry whether the test is being retried in isolation, so it has already been reported as started.
     * @param reportFailure whether a failed test is reported, or only disposed, to be retried in isolation.
     */
    private fun runNonGroupingStage(
        method: TestMethodTestDescriptor,
        context: JupiterEngineExecutionContext,
        isRetry: Boolean = false,
        reportFailure: Boolean = true,
    ): TestMethodInfo? {
        val methodContext = method.prepare(context)

        // Honor JUnit `ExecutionCondition` extensions (e.g. the filter selecting which tests must run,
        // `SmokeTestExecutionCondition`) before running anything. The standard Jupiter engine evaluates
        // `shouldBeSkipped` for every test as part of its node lifecycle, but this custom engine drives
        // method execution directly, so we have to replicate that check here. Without it, conditionally
        // disabled tests (e.g. tests muted in smoke mode) would always run.
        val skipResult = method.shouldBeSkipped(methodContext)
        if (skipResult.isSkipped) {
            val reason = skipResult.reason.orElse("<unknown reason>")
            if (isRetry) {
                // A started test can only be finished.
                methodContext.executionListener.executionFinished(method, TestExecutionResult.aborted(TestAbortedException(reason)))
            } else {
                methodContext.executionListener.executionSkipped(method, reason)
            }
            return null
        }

        if (isRetry) {
            // The test instance is created by `prepare`, while the test is configured by `execute`.
            methodContext.extensionContext.testInstances
                .flatMap { it.findInstance(AbstractTwoStageKotlinCompilerTestBase::class.java) }
                .ifPresent { it.isRetriedInIsolation = true }
        } else {
            methodContext.executionListener.executionStarted(method)
        }
        methodContext.throwableCollector.executeCatchingAll { method.execute(methodContext, DynamicTestExecutorStub) }
        val testInstance = methodContext.extensionContext.testInstances
            .flatMap { it.findInstance(AbstractTwoStageKotlinCompilerTestBase::class.java) }
            .orElse(null)

        // If there is no `@TestMetadata` annotation, then this is some utility test (like `testAllFilesPresentIn`)
        // and so it should be excluded in grouping processing. Without a test instance, the test has failed to start.
        if (testInstance == null || method.testMethod.annotations.none { it is TestMetadata }) {
            methodContext.executionListener.executionFinished(
                method,
                methodContext.throwableCollector.toTestExecutionResult()
            )
            return null
        }
        val info = TestMethodInfo(method, methodContext, testInstance).apply {
            if (finishIfFailed(reportFailure)) return@apply
            nonGroupingStageThrowableCollector.executeCatchingAll {
                val testRunner = testInstance.nonGroupingRunner
                testRunner.runTestPreprocessing()
                testRunner.runSteps()
                hadIgnoredFailuresOnNonGroupingStage = testRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            }
            finishIfFailed(reportFailure)
        }
        return info
    }

    /**
     * @return the tests to retry in isolation (see [retryInIsolation]), which are disposed, but not reported: all tests
     *   of the batch if its grouping stage failed as a whole, which says nothing about its individual tests, otherwise
     *   the tests which failed in the batch. A grouping stage which was aborted, rather than failed, is not retried:
     *   its tests are reported as aborted. Unless [retryFailures] is `false`: then nothing is retried, and the tests
     *   are reported with their own failures, or with the batch failure.
     */
    private fun runGroupingStageOnBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: List<TestMethodInfo>,
        index: Int,
        retryFailures: Boolean,
    ): List<TestMethodInfo> {
        require(batch.size > 1) { "Batch expected to have at least 2 methods, got ${batch.size}" }
        val testDescriptor = GroupingStageTestDescriptor(
            uniqueId = classDescriptor.uniqueId.append("dynamic-test", "batch$index"),
            displayName = "Grouped batch #$index"
        )
        // Batches within one class run concurrently, and `TestDescriptor` doesn't promise a thread-safe `addChild()`,
        // albeit its only implementation adds the child into a synchronized set, but we should not rely on it.
        synchronized(classDescriptor) { classDescriptor.addChild(testDescriptor) }
        val throwableCollector = createThrowableCollector()
        val someTestInstance = batch.first().testInstance

        val executionListener = context.executionListener
        executionListener.dynamicTestRegistered(testDescriptor)
        executionListener.executionStarted(testDescriptor)
        throwableCollector.executeCatchingAll {
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
        val batchFailure = throwableCollector.throwable
        val batchResult = throwableCollector.toTestExecutionResult()
        if (batchResult.status == TestExecutionResult.Status.FAILED && retryFailures) {
            // The batch is reported as aborted (it shows up as skipped, with the reason attached), and its tests are retried.
            executionListener.executionFinished(testDescriptor, TestExecutionResult.aborted(batchFailure))
            batch.forEach { it.disposeRunners() }
            println("Grouping engine: grouped batch #$index of ${batch.size} tests could not be run as a group, re-running its tests in isolated batches")
            return batch
        }
        for (methodInfo in batch) {
            methodInfo.context.throwableCollector.executeCatchingAll {
                // A test is checked for unmuting only if the grouping stage has not failed.
                methodInfo.testInstance.nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = batchFailure == null)
            }
        }

        executionListener.executionFinished(testDescriptor, batchResult)
        val failedTests = mutableListOf<TestMethodInfo>()
        for (methodInfo in batch) {
            methodInfo.disposeRunners()
            if (methodInfo.failedWithError && retryFailures) {
                failedTests += methodInfo
                continue
            }
            // A test which failed on its own is reported with its own failure, the others with the batch failure.
            val result = if (methodInfo.failed) methodInfo.nonGroupingStageThrowableCollector.toTestExecutionResult() else batchResult
            methodInfo.context.executionListener.executionFinished(methodInfo.descriptor, result)
        }
        return failedTests
    }

    /**
     * @param reportFailure whether a failed test is reported, or only disposed, to be retried in isolation.
     * @return `false` if the test failed and is not reported.
     */
    private fun runGroupingStageOnSingleSizedBatch(testInfo: TestMethodInfo, reportFailure: Boolean): Boolean {
        val throwableCollector = testInfo.nonGroupingStageThrowableCollector
        val testInstance = testInfo.testInstance
        throwableCollector.executeCatchingAll {
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

        testInfo.disposeRunners()
        if (testInfo.failedWithError && !reportFailure) return false
        testInfo.reportFinished(throwableCollector)
        return true
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

/**
 * Thread-safe wrapper around EngineExecutionListener.
 * Gradle's execution listener is not thread-safe, so all calls must be synchronized.
 */
private class SynchronizedEngineExecutionListener(
    private val delegate: EngineExecutionListener,
) : EngineExecutionListener {
    private val started = HashSet<TestDescriptor?>()
    private val finished = HashSet<TestDescriptor?>()

    override fun dynamicTestRegistered(testDescriptor: TestDescriptor?) = synchronized(this) {
        delegate.dynamicTestRegistered(testDescriptor)
    }

    override fun executionStarted(testDescriptor: TestDescriptor?) = synchronized(this) {
        started += testDescriptor
        delegate.executionStarted(testDescriptor)
    }

    override fun executionSkipped(testDescriptor: TestDescriptor?, reason: String?) = synchronized(this) {
        finished += testDescriptor
        delegate.executionSkipped(testDescriptor, reason)
    }

    override fun executionFinished(testDescriptor: TestDescriptor?, testExecutionResult: TestExecutionResult?) = synchronized(this) {
        finished += testDescriptor
        delegate.executionFinished(testDescriptor, testExecutionResult)
    }

    /** Reports [testDescriptor] as failed with [throwable], unless it has already been reported. */
    fun failIfUnfinished(testDescriptor: TestDescriptor, throwable: Throwable) {
        synchronized(this) {
            if (testDescriptor in finished) return
            if (testDescriptor !in started) executionStarted(testDescriptor)
            executionFinished(testDescriptor, TestExecutionResult.failed(throwable))
        }
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

/**
 * Unlike [ThrowableCollector.execute], which deliberately rethrows unrecoverable throwables such as `OutOfMemoryError`,
 * collects everything, so that a test or a batch running out of memory fails alone, and is disposed and reported as
 * usual, rather than taking the whole run down with it. An unrecoverable throwable is collected wrapped, as the
 * collector would rethrow it again otherwise.
 */
private inline fun ThrowableCollector.executeCatchingAll(crossinline block: () -> Unit) {
    try {
        execute { block() }
    } catch (e: Throwable) {
        execute { throw UnrecoverableFailure(e) }
    }
}

private class UnrecoverableFailure(cause: Throwable) : Exception("Unrecoverable failure: $cause", cause)

private fun JupiterEngineExecutionContext.failIfUnfinished(testDescriptor: TestDescriptor, throwable: Throwable) =
    (executionListener as SynchronizedEngineExecutionListener).failIfUnfinished(testDescriptor, throwable)

private class TestMethodWithContext(val method: TestMethodTestDescriptor, val context: JupiterEngineExecutionContext)

/** @property isolated whether the tests are isolated by their batch tokens, so they run alone as if no batching happened. */
private class PlannedBatch(val tests: List<TestMethodWithContext>, val isolated: Boolean)

private fun AbstractTwoStageKotlinCompilerTestBase.computeBatchTokens(): List<BatchToken> {
    val testConfiguration = nonGroupingRunner.testConfiguration
    return testConfiguration.groupingTestIsolators.mapNotNull {
        it.computeBatchToken(testConfiguration.testServices.moduleStructure).takeIf { token -> token != BatchToken.Regular }
    }
}

private data class TestMethodInfo(
    val descriptor: TestMethodTestDescriptor,
    val context: JupiterEngineExecutionContext,
    val testInstance: AbstractTwoStageKotlinCompilerTestBase,
) {
    val failed: Boolean
        get() = nonGroupingStageThrowableCollector.isNotEmpty

    /** Whether the test has failed, rather than been aborted, like skipped by a test configurator: only such a test is retried. */
    val failedWithError: Boolean
        get() = nonGroupingStageThrowableCollector.toTestExecutionResult().status == TestExecutionResult.Status.FAILED

    var hadIgnoredFailuresOnNonGroupingStage: Boolean = false

    var disposed: Boolean = false
        private set

    val nonGroupingStageThrowableCollector: ThrowableCollector
        get() = context.throwableCollector

    /**
     * Disposes both runners of the test. The grouping-stage runner of a test has either run the grouping stage of its
     * batch, which is over by now, or has not been used at all, so there is nothing to keep it alive for.
     */
    fun disposeRunners() {
        if (disposed) return
        nonGroupingStageThrowableCollector.executeCatchingAll {
            if (testInstance.nonGroupingStageRunnerInitialized) testInstance.nonGroupingRunner.finalizeAndDispose()
        }
        nonGroupingStageThrowableCollector.executeCatchingAll {
            if (testInstance.secondStageRunnerInitialized) testInstance.groupingStageRunner.finalizeAndDispose()
        }
        disposed = true
    }
}

/**
 * @param reportFailure whether a failed test is reported, or only disposed, to be retried in isolation.
 *   An aborted test, or a test which has only ignored failures, is reported anyway.
 * @returns true if the test failed
 */
private fun TestMethodInfo.finishIfFailed(reportFailure: Boolean): Boolean {
    return (failed || hadIgnoredFailuresOnNonGroupingStage).also {
        if (it) {
            disposeRunners()
            if (reportFailure || !failedWithError) reportFinished(nonGroupingStageThrowableCollector)
        }
    }
}

private fun TestMethodInfo.reportFinished(throwableCollector: ThrowableCollector) {
    context.executionListener.executionFinished(descriptor, throwableCollector.toTestExecutionResult())
}
