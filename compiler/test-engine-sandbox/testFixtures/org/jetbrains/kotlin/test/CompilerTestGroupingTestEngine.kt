/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

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
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.hierarchical.Node
import org.junit.platform.engine.support.hierarchical.ThrowableCollector
import java.util.concurrent.Future

class CompilerTestGroupingTestEngine : TestEngine {
    companion object {
        const val ID = "kotlin-compiler-grouping-engine"
    }

    override fun getId(): String = ID

    override fun execute(request: ExecutionRequest) {
        val baseContext = JupiterEngineExecutionContext(request.engineExecutionListener, getJupiterConfiguration(request))
        request.rootTestDescriptor.traverseClasses(baseContext) { context, classDescriptor ->
            val methods = classDescriptor.children.filterIsInstance<TestMethodTestDescriptor>()
            val testInfos = methods.map { method ->
                val methodContext = method.prepare(context)
                methodContext.executionListener.executionStarted(method)
                methodContext.throwableCollector.execute { method.execute(methodContext, DynamicTestExecutorStub) }
                val testInstance = methodContext.extensionContext
                    .requiredTestInstances
                    .findInstance(AbstractTwoStageKotlinCompilerTest::class.java)
                    .get()
                TestMethodInfo(method, methodContext, testInstance)
            }

            testInfos.forEach { it.runNonGroupingPhase() }
            runGroupingPhase(context, classDescriptor, testInfos)
        }
    }

    private fun runGroupingPhase(context: JupiterEngineExecutionContext, classDescriptor: TestDescriptor, tests: List<TestMethodInfo>) {
        val successfulTests = tests.filterNot { it.failed }
        if (successfulTests.isEmpty()) return
        val batches = groupTestsInBatches(successfulTests)
        batches.forEachIndexed { index, batch ->
            runGroupingPhaseOnBatch(context, classDescriptor, batch, index)
        }
    }

    private fun groupTestsInBatches(infos: List<TestMethodInfo>): List<List<TestMethodInfo>> {
        return listOf(infos)
    }

    private fun runGroupingPhaseOnBatch(
        context: JupiterEngineExecutionContext,
        classDescriptor: TestDescriptor,
        batch: List<TestMethodInfo>,
        index: Int,
    ) {
        val testDescriptor = GroupingPhaseTestDescriptor(
            uniqueId = batch.first().descriptor.uniqueId.removeLastSegment().append("dynamic-test", "batch"),
            displayName = "Grouped batch #${index + 1}"
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
                    catchingExecutor = { block ->
                        methodInfo.context.throwableCollector.execute(block)
                    }
                )
            }
            testRunner.run(nonGroupingPhaseOutputs)
            testRunner.reportFailures()
        }
        executionListener.executionFinished(testDescriptor, throwableCollector.toTestExecutionResult())
        batch.forEach {
            it.finalizeNonGroupingPhase()
            it.updateFailed()
            val collector = if (it.failed) it.nonGroupingPhaseThrowableCollector else throwableCollector
            it.reportFinished(collector)
        }
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

        // Filter: keep only test classes annotated with @UseBatchingEngine (directly or inherited)
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
        // Always include container nodes (they'll be filtered recursively)
        if (descriptor.isContainer && descriptor.children.isNotEmpty()) {
            return true
        }

        if (descriptor.isContainer) {
            val testClass = (descriptor as? ClassTestDescriptor)?.testClass
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

    private fun TestDescriptor.traverseClasses(
        baseContext: JupiterEngineExecutionContext,
        block: (JupiterEngineExecutionContext, TestDescriptor) -> Unit,
    ) {
        if (!this.isContainer) return
        @Suppress("UNCHECKED_CAST")
        val context = (this as? Node<JupiterEngineExecutionContext>)?.prepare(baseContext) ?: baseContext
        val needReport = this !is JupiterEngineDescriptor
        if (needReport) context.executionListener.executionStarted(this)
        if (children.any { it.isTest }) {
            block(context, this)
        }
        for (child in children) {
            if (child.isContainer) {
                child.traverseClasses(context, block)
            }
        }
        if (needReport) context.executionListener.executionFinished(this, context.throwableCollector.toTestExecutionResult())
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
    val testInstance: AbstractTwoStageKotlinCompilerTest,
) {
    var failed: Boolean = false
        private set

    var finalized: Boolean = false
        private set

    val nonGroupingPhaseThrowableCollector: ThrowableCollector
        get() = context.throwableCollector

    fun updateFailed() {
        failed = failed || nonGroupingPhaseThrowableCollector.isNotEmpty
    }

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

private fun TestMethodInfo.runNonGroupingPhase() {
    nonGroupingPhaseThrowableCollector.execute {
        val testRunner = testInstance.nonGroupingRunner
        testRunner.runTestPreprocessing()
        testRunner.runSteps()
        testRunner.reportFailures()
    }
    updateFailed()
    if (failed) {
        finalizeNonGroupingPhase()
        reportFinished(nonGroupingPhaseThrowableCollector)
    }
}

private fun TestMethodInfo.reportFinished(throwableCollector: ThrowableCollector) {
    context.executionListener.executionFinished(descriptor, throwableCollector.toTestExecutionResult())
}
