/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.kotlin.test.DynamicWithMaxThresholdParallelExecutionConfigurationStrategy
import org.jetbrains.kotlin.test.NonGroupingPhaseOutput
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.junit.jupiter.engine.config.CachingJupiterConfiguration
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration
import org.junit.jupiter.engine.config.JupiterConfiguration
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.iterator

class CompilerTestGroupingTestEngine : TestEngine {
    companion object {
        const val ID = "kotlin-compiler-grouping-engine"

        private const val GROUPING_ENGINE_POOL_SIZE_PROP = "kotlin.test.grouping.engine.pool.size"

        private const val SIMULTANEOUS_METHODS_GROUPS_PROP = "kotlin.test.grouping.engine.simultaneous.methods.groups"
        private const val DEFAULT_SIMULTANEOUS_METHODS_GROUPS = 2

        private const val SIMULTANEOUS_METHODS_GROUP_SIZE_PROP = "kotlin.test.grouping.engine.simultaneous.methods.group.size"
        private const val DEFAULT_SIMULTANEOUS_METHODS_GROUP_SIZE = 50
    }

    override fun getId(): String = ID

    private class ExecutionContext(params: ConfigurationParameters) : Closeable {
        private val workerPool: ExecutorService
        val dispatcher: ExecutorCoroutineDispatcher
        val activeClasses: Semaphore
        val methodsChunkSize: Int

        init {
            val parallelism = DynamicWithMaxThresholdParallelExecutionConfigurationStrategy.computeParallelism(
                params,
                GROUPING_ENGINE_POOL_SIZE_PROP,
                DynamicWithMaxThresholdParallelExecutionConfigurationStrategy.FIXED_THRESHOLD_PROP
            )
            workerPool = Executors.newFixedThreadPool(parallelism)
            dispatcher = workerPool.asCoroutineDispatcher()

            val numberOfActiveClasses = params.get(SIMULTANEOUS_METHODS_GROUPS_PROP).orElse(null)?.toInt()?.also {
                require(it > 0) { "Number of simultaneous method groups must be positive, but was $it" }
            } ?: DEFAULT_SIMULTANEOUS_METHODS_GROUPS

            activeClasses = Semaphore(permits = numberOfActiveClasses)

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
            val synchronizedListener = SynchronizedEngineExecutionListener(request.engineExecutionListener)
            val baseContext = JupiterEngineExecutionContext(synchronizedListener, getJupiterConfiguration(request))
            runBlocking(ctx.dispatcher) {
                traverseClasses(request.rootTestDescriptor, baseContext) { context, classDescriptor ->
                    launch {
                        context(ctx) { processClass(context, classDescriptor) }
                    }
                }?.join()
            }
        }
    }

    private fun CoroutineScope.traverseClasses(
        descriptor: TestDescriptor,
        baseContext: JupiterEngineExecutionContext,
        block: (JupiterEngineExecutionContext, TestDescriptor) -> Job,
    ): Job? {
        if (!descriptor.isContainer) return null
        @Suppress("UNCHECKED_CAST")
        val context = (descriptor as? Node<JupiterEngineExecutionContext>)?.prepare(baseContext) ?: baseContext
        val needReport = descriptor !is JupiterEngineDescriptor
        if (needReport) context.executionListener.executionStarted(descriptor)
        val testClassJob = runIf(descriptor.children.any { it.isTest }) {
            block(context, descriptor)
        }
        val childJobs = descriptor.children.mapNotNull { child ->
            runIf(child.isContainer) {
                traverseClasses(child, context, block)
            }
        }
        return runIf(needReport) {
            launch {
                testClassJob?.join()
                childJobs.joinAll()
                context.executionListener.executionFinished(descriptor, context.throwableCollector.toTestExecutionResult())
            }
        }
    }

    context(ctx: ExecutionContext)
    private suspend fun CoroutineScope.processClass(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
    ) {
        val methods = classDescriptor.children.filterIsInstance<TestMethodTestDescriptor>()
        val groupCounter = AtomicInteger(1)
        val groupJobs = methods.chunked(ctx.methodsChunkSize).map { methodsChunk ->
            launch {
                ctx.activeClasses.withPermit {
                    val testInfosFutures = methodsChunk.map { method ->
                        async {
                            runNonGroupingPhase(method, context)
                        }
                    }
                    val testInfos = testInfosFutures.awaitAll().filterNotNull()
                    runGroupingPhase(context, classDescriptor, testInfos, groupCounter)
                }
            }
        }
        groupJobs.joinAll()
    }

    private fun runNonGroupingPhase(
        method: TestMethodTestDescriptor,
        context: JupiterEngineExecutionContext,
    ): TestMethodInfo? {
        val methodContext = method.prepare(context)
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
            nonGroupingPhaseThrowableCollector.execute {
                val testRunner = testInstance.nonGroupingRunner
                testRunner.runTestPreprocessing()
                testRunner.runSteps()
                hadIgnoredFailuresOnNonGroupingPhase = testRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            }
            finishIfFailed()
        }
        return info
    }

    private suspend fun CoroutineScope.runGroupingPhase(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        tests: List<TestMethodInfo>,
        groupCounter: AtomicInteger,
    ) {
        val successfulTests = tests.filterNot { it.failed || it.hadIgnoredFailuresOnNonGroupingPhase }
        if (successfulTests.isEmpty()) return
        val batches = groupTestsInBatches(successfulTests)

        val batchFutures = batches.map { batch ->
            launch {
                if (batch.size == 1) {
                    runGroupingPhaseOnSingleSizedBatch(batch.single())
                } else {
                    runGroupingPhaseOnBatch(context, classDescriptor, batch, index = groupCounter.getAndIncrement())
                }
            }
        }

        batchFutures.joinAll()
    }

    private fun groupTestsInBatches(infos: List<TestMethodInfo>): List<List<TestMethodInfo>> {
        val groupedByTokens = infos.groupBy { info ->
            val testConfiguration = info.testInstance.nonGroupingRunner.testConfiguration
            testConfiguration.groupingTestIsolators.mapNotNull {
                it.computeBatchToken(testConfiguration.testServices.moduleStructure).takeIf { token -> token != BatchToken.Regular }
            }
        }

        return buildList {
            for ((tokens, batch) in groupedByTokens) {
                if (BatchToken.Isolated in tokens) {
                    for (info in batch) {
                        add(listOf(info))
                    }
                } else {
                    add(batch)
                }
            }
        }
    }

    private fun runGroupingPhaseOnBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: List<TestMethodInfo>,
        index: Int,
    ) {
        require(batch.size > 1) { "Batch expected to have at least 2 methods, got ${batch.size}" }
        val testDescriptor = GroupingPhaseTestDescriptor(
            uniqueId = batch.first().descriptor.uniqueId.removeLastSegment().append("dynamic-test", "batch$index"),
            displayName = "Grouped batch #$index"
        )
        classDescriptor.addChild(testDescriptor)
        val throwableCollector = createThrowableCollector()
        val someTestInstance = batch.first().testInstance

        val executionListener = context.executionListener
        executionListener.dynamicTestRegistered(testDescriptor)
        executionListener.executionStarted(testDescriptor)
        throwableCollector.execute {
            val testRunner = someTestInstance.groupingPhaseRunner
            val nonGroupingPhaseOutputs = batch.map { methodInfo ->
                NonGroupingPhaseOutput(
                    testServices = methodInfo.testInstance.nonGroupingRunner.testServices,
                    catchingExecutor = { wrapper, block ->
                        methodInfo.testInstance.nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                    }
                )
            }
            testRunner.run(nonGroupingPhaseOutputs)
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
            it.finalizeNonGroupingPhase()
            val collector = if (it.failed) it.nonGroupingPhaseThrowableCollector else throwableCollector
            it.reportFinished(collector)
        }
    }

    private fun runGroupingPhaseOnSingleSizedBatch(testInfo: TestMethodInfo) {
        val throwableCollector = testInfo.nonGroupingPhaseThrowableCollector
        val testInstance = testInfo.testInstance
        throwableCollector.execute {
            val groupingRunner = testInstance.groupingPhaseRunner
            val nonGroupingRunner = testInstance.nonGroupingRunner
            val nonGroupingPhaseOutput = NonGroupingPhaseOutput(
                testServices = testInstance.nonGroupingRunner.testServices,
                catchingExecutor = { wrapper, block ->
                    nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                }
            )
            groupingRunner.run(listOf(nonGroupingPhaseOutput))

            /*
             * Exceptions from facades were reported to the failures interceptor of the grouping runner.
             * However, failure suppressors should be run from non-grouping runner, as they need access to
             * the real module structure of the specific test to be able to extract directives from there.
             */
            nonGroupingRunner.failuresInterceptor += groupingRunner.failuresInterceptor
            nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
        }

        testInfo.finalizeNonGroupingPhase()
        testInfo.reportFinished(throwableCollector)
    }

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val configuration = CachingJupiterConfiguration(
            DefaultJupiterConfiguration(
                discoveryRequest.configurationParameters,
                discoveryRequest.outputDirectoryProvider
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

private class GroupingPhaseTestDescriptor(
    uniqueId: UniqueId,
    displayName: String,
) : AbstractTestDescriptor(uniqueId, displayName, /* source = */ null) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
}

private data class TestMethodInfo(
    val descriptor: TestMethodTestDescriptor,
    val context: JupiterEngineExecutionContext,
    val testInstance: AbstractTwoStageKotlinCompilerTestBase,
) {
    val failed: Boolean
        get() = nonGroupingPhaseThrowableCollector.isNotEmpty

    var hadIgnoredFailuresOnNonGroupingPhase: Boolean = false

    var finalized: Boolean = false
        private set

    val nonGroupingPhaseThrowableCollector: ThrowableCollector
        get() = context.throwableCollector

    fun finalizeNonGroupingPhase() {
        if (finalized) return
        nonGroupingPhaseThrowableCollector.execute {
            if (testInstance.nonGroupingPhaseRunnerInitialized) {
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
    return (failed || hadIgnoredFailuresOnNonGroupingPhase).also {
        if (it) {
            finalizeNonGroupingPhase()
            reportFinished(nonGroupingPhaseThrowableCollector)
        }
    }
}

private fun TestMethodInfo.reportFinished(throwableCollector: ThrowableCollector) {
    context.executionListener.executionFinished(descriptor, throwableCollector.toTestExecutionResult())
}
