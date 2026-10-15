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
    }

    override fun getId(): String = ID

    private class ExecutionContext(params: ConfigurationParameters) : Closeable {
        private val workerPool: ExecutorService
        val dispatcher: ExecutorCoroutineDispatcher
        val activeBatches: Semaphore
        val methodsChunkSize: Int

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
                        ctx.activeBatches.withPermit { executeBatch(classContext, classDescriptor, batch, index = index + 1) }
                    }
                }
            }
        } finally {
            // The tests of a nested class are spread over the batches of the whole top-level class,
            // so the containers are finished, bottom-up, only once all of them are done.
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
    private suspend fun planBatches(methods: List<TestMethodWithContext>): List<List<TestMethodWithContext>> {
        val tokens = coroutineScope { methods.map { async { planTokens(it.method, it.context) } }.awaitAll() }
        val plannedMethods = methods.zip(tokens).mapNotNull { [method, tokens] -> tokens?.let { method to it } }
        return plannedMethods.groupBy({ it.second }, { it.first }).flatMap { [tokens, sameTokenMethods] ->
            // Sorted by unique id, so that which tests share a batch does not depend on the order JUnit lists them in.
            val sortedMethods = sameTokenMethods.sortedBy { it.method.uniqueId.toString() }
            if (BatchToken.Isolated in tokens) sortedMethods.map { listOf(it) } else sortedMethods.chunked(ctx.methodsChunkSize)
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
        collector.execute { method.execute(methodContext, DynamicTestExecutorStub) }
        val testInstance = methodContext.extensionContext.testInstances
            .flatMap { it.findInstance(AbstractTwoStageKotlinCompilerTestBase::class.java) }
            .orElse(null)
        if (testInstance != null) {
            if (collector.isEmpty) collector.execute { tokens = testInstance.computeBatchTokens() }
            collector.execute { if (testInstance.nonGroupingStageRunnerInitialized) testInstance.nonGroupingRunner.finalizeAndDispose() }
            collector.execute { if (testInstance.secondStageRunnerInitialized) testInstance.groupingStageRunner.finalizeAndDispose() }
        }
        if (collector.isEmpty) return tokens
        methodContext.executionListener.executionStarted(method)
        methodContext.executionListener.executionFinished(method, collector.toTestExecutionResult())
        return null
    }

    private suspend fun executeBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: List<TestMethodWithContext>,
        index: Int,
    ) {
        val testInfos = coroutineScope { batch.map { async { runNonGroupingStage(it.method, it.context) } }.awaitAll() }
        val successfulTests = testInfos.filterNotNull().filterNot { it.failed || it.hadIgnoredFailuresOnNonGroupingStage }
        when (successfulTests.size) {
            0 -> {}
            1 -> runGroupingStageOnSingleSizedBatch(successfulTests.single())
            else -> runGroupingStageOnBatch(context, classDescriptor, successfulTests, index)
        }
    }

    private fun runNonGroupingStage(
        method: TestMethodTestDescriptor,
        context: JupiterEngineExecutionContext,
    ): TestMethodInfo? {
        val methodContext = method.prepare(context)

        // Honor JUnit `ExecutionCondition` extensions (e.g. the filter selecting which tests must run,
        // `SmokeTestExecutionCondition`) before running anything. The standard Jupiter engine evaluates
        // `shouldBeSkipped` for every test as part of its node lifecycle, but this custom engine drives
        // method execution directly, so we have to replicate that check here. Without it, conditionally
        // disabled tests (e.g. tests muted in smoke mode) would always run.
        val skipResult = method.shouldBeSkipped(methodContext)
        if (skipResult.isSkipped) {
            methodContext.executionListener.executionSkipped(method, skipResult.reason.orElse("<unknown reason>"))
            return null
        }

        methodContext.executionListener.executionStarted(method)
        methodContext.throwableCollector.execute { method.execute(methodContext, DynamicTestExecutorStub) }
        val testInstance = methodContext.extensionContext
            .requiredTestInstances
            .findInstance(AbstractTwoStageKotlinCompilerTestBase::class.java)
            .get()

        // If there is no `@TestMetadata` annotation, then this is some utility test (like `testAllFilesPresentIn`)
        // and so it should be excluded in grouping processing.
        if (method.testMethod.annotations.none { it is TestMetadata }) {
            methodContext.executionListener.executionFinished(
                method,
                methodContext.throwableCollector.toTestExecutionResult()
            )
            return null
        }
        val info = TestMethodInfo(method, methodContext, testInstance).apply {
            if (finishIfFailed()) return@apply
            nonGroupingStageThrowableCollector.execute {
                val testRunner = testInstance.nonGroupingRunner
                testRunner.runTestPreprocessing()
                testRunner.runSteps()
                hadIgnoredFailuresOnNonGroupingStage = testRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            }
            finishIfFailed()
        }
        return info
    }

    private fun runGroupingStageOnBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: List<TestMethodInfo>,
        index: Int,
    ) {
        require(batch.size > 1) { "Batch expected to have at least 2 methods, got ${batch.size}" }
        val testDescriptor = GroupingStageTestDescriptor(
            uniqueId = classDescriptor.uniqueId.append("dynamic-test", "batch$index"),
            displayName = "Grouped batch #$index"
        )
        synchronized(classDescriptor) { classDescriptor.addChild(testDescriptor) }
        val throwableCollector = createThrowableCollector()
        val someTestInstance = batch.first().testInstance

        val executionListener = context.executionListener
        executionListener.dynamicTestRegistered(testDescriptor)
        executionListener.executionStarted(testDescriptor)
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
        for (methodInfo in batch) {
            methodInfo.context.throwableCollector.execute {
                methodInfo.testInstance.nonGroupingRunner.failuresInterceptor.reportFailures(
                    // we need to check for unmuting only if there were no exceptions from the
                    // grouped facades
                    checkForUnmuting = throwableCollector.isEmpty
                )
            }
        }

        executionListener.executionFinished(testDescriptor, throwableCollector.toTestExecutionResult())
        batch.forEach {
            it.finalizeNonGroupingStage()
            val collector = if (it.failed) it.nonGroupingStageThrowableCollector else throwableCollector
            it.reportFinished(collector)
        }
    }

    private fun runGroupingStageOnSingleSizedBatch(testInfo: TestMethodInfo) {
        val throwableCollector = testInfo.nonGroupingStageThrowableCollector
        val testInstance = testInfo.testInstance
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

        testInfo.finalizeNonGroupingStage()
        testInfo.reportFinished(throwableCollector)
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

private class TestMethodWithContext(val method: TestMethodTestDescriptor, val context: JupiterEngineExecutionContext)

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

    var hadIgnoredFailuresOnNonGroupingStage: Boolean = false

    var finalized: Boolean = false
        private set

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
}

/**
 * @returns true if the test failed
 */
private fun TestMethodInfo.finishIfFailed(): Boolean {
    return (failed || hadIgnoredFailuresOnNonGroupingStage).also {
        if (it) {
            finalizeNonGroupingStage()
            reportFinished(nonGroupingStageThrowableCollector)
        }
    }
}

private fun TestMethodInfo.reportFinished(throwableCollector: ThrowableCollector) {
    context.executionListener.executionFinished(descriptor, throwableCollector.toTestExecutionResult())
}
