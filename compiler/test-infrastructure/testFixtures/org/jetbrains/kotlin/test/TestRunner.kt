/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectivesImpl
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.IOException

sealed class TestRunner<Step : TestStep<*, *>, Configuration : TestConfiguration<Step>>(val testConfiguration: Configuration) {
    val testServices: TestServices get() = testConfiguration.testServices
    protected val allFailedExceptions = mutableListOf<WrappedException>()

    open fun runTestPreprocessing() {
        val moduleStructure = testServices.moduleStructure

        val modules = moduleStructure.modules
        val artifactsProvider = ArtifactsProvider(testServices, modules)
        testServices.registerArtifactsProvider(artifactsProvider)

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            preprocessor.preprocessModuleStructure(moduleStructure)
        }

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            withAssertionCatching(WrappedException::FromPreAnalysisHandler) {
                preprocessor.prepareSealedClassInheritors(moduleStructure)
            }
        }
    }

    fun reportFailures() {
        val filteredFailedAssertions = filterFailedExceptions(allFailedExceptions)
        filteredFailedAssertions.firstIsInstanceOrNull<WrappedException.FromFacade>()?.let {
            throw it
        }
        testServices.assertions.failAll(filteredFailedAssertions)
    }

    fun finalizeAndDispose(beforeDispose: (Configuration) -> Unit = {}) {
        try {
            testConfiguration.testServices.temporaryDirectoryManager.cleanupTemporaryDirectories()
        } catch (e: IOException) {
            println("Failed to clean temporary directories:")
            e.printStackTrace()
        }
        beforeDispose(testConfiguration)
        disposeRootInWriteAction(testConfiguration.rootDisposable)
    }

    protected fun interface RunStep<Step : TestStep<*, *>> {
        fun run(
            step: Step,
            inputArtifact: ResultingArtifact<*>,
            thereWereCriticalExceptionsOnPreviousSteps: Boolean
        ): TestStep.StepResult<*>
    }

    protected fun runPipelineOnSingleUnit(
        produceStartingArtifact: () -> ResultingArtifact<*>,
        shouldRunStep: (Step, ResultingArtifact<*>) -> Boolean,
        runStep: RunStep<Step>,
        onArtifactResult: (ResultingArtifact<*>) -> Unit,
        onHandlersResult: (Step) -> Unit
    ): Boolean {
        var inputArtifact = produceStartingArtifact()

        for (step in testConfiguration.steps) {
            if (!shouldRunStep(step, inputArtifact)) continue

            val thereWereCriticalExceptionsOnPreviousSteps = allFailedExceptions.any { it.failureDisablesNextSteps }
            when (val result = runStep.run(step, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)) {
                is TestStep.StepResult.Artifact<*> -> {
                    require(step is TestStep.FacadeStep<*, *>)
                    onArtifactResult(result.outputArtifact)
                    inputArtifact = result.outputArtifact
                }
                is TestStep.StepResult.ErrorFromFacade -> {
                    allFailedExceptions += result.exception
                    return false
                }
                is TestStep.StepResult.HandlersResult -> {
                    val (exceptionsFromHandlers, shouldRunNextSteps) = result
                    allFailedExceptions += exceptionsFromHandlers
                    onHandlersResult(step)
                    if (!shouldRunNextSteps) {
                        return false
                    }
                }
                is TestStep.StepResult.NoArtifactFromFacade -> return false
            }
        }
        return true
    }

    protected fun filterFailedExceptions(failedExceptions: List<WrappedException>): List<Throwable> {
        return testConfiguration.afterAnalysisCheckers
            .fold(failedExceptions) { assertions, checker ->
                checker.suppressIfNeeded(assertions)
            }
            .sorted()
            .map { it.cause }
    }

    /*
     * Returns true if there was an exception in block
     */
    protected inline fun withAssertionCatching(exceptionWrapper: (Throwable) -> WrappedException, block: () -> Unit): Boolean {
        return try {
            block()
            false
        } catch (e: Throwable) {
            allFailedExceptions += exceptionWrapper(e)
            true
        }
    }
}

class NonGroupingTestRunner(
    testConfiguration: NonGroupingPhaseTestConfiguration
) : TestRunner<TestStep.NonGroupingStep<*, *>, NonGroupingPhaseTestConfiguration>(testConfiguration) {
    companion object {
        fun AnalysisHandler<*>.shouldRun(thereWasAnException: Boolean): Boolean {
            return !(doNotRunIfThereWerePreviousFailures && thereWasAnException)
        }
    }

    private val allRanHandlers = mutableSetOf<AnalysisHandler<*>>()

    fun runTest(@TestDataFile testDataFileName: String, beforeDispose: (NonGroupingPhaseTestConfiguration) -> Unit = {}) {
        try {
            prepareModuleStructure(testDataFileName) ?: return
            runTestPipeline()
        } finally {
            finalizeAndDispose(beforeDispose)
        }
    }

    fun prepareModuleStructure(testDataFileName: String): TestModuleStructure? {
        val services = testServices

        @Suppress("NAME_SHADOWING")
        val testDataFileName = testConfiguration.metaTestConfigurators.fold(testDataFileName) { fileName, configurator ->
            configurator.transformTestDataPath(fileName)
        }

        val moduleStructure = try {
            testConfiguration.moduleStructureExtractor.splitTestDataByModules(
                testDataFileName,
                testConfiguration.directives,
            ).also {
                services.register(TestModuleStructure::class, it)
            }
        } catch (e: ExceptionFromModuleStructureTransformer) {
            services.register(TestModuleStructure::class, e.alreadyParsedModuleStructure)
            val exception = filterFailedExceptions(
                listOf(WrappedException.FromModuleStructureTransformer(e.cause))
            ).firstOrNull() ?: return null
            throw exception
        }

        testConfiguration.metaTestConfigurators.forEach {
            services.assertions.assumeFalse(it.shouldSkipTest()) { "Test skipped by ${it::class.simpleName}" }
        }
        return moduleStructure
    }

    fun runTestPipeline() {
        runTestPreprocessing()
        runSteps()
        reportFailures()
    }

    override fun runTestPreprocessing() {
        super.runTestPreprocessing()
        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
        globalMetadataInfoHandler.parseExistingMetadataInfosFromAllSources()
    }

    fun runSteps() {
        val services = testConfiguration.testServices
        val moduleStructure = services.moduleStructure

        for (module in moduleStructure.modules) {
            val shouldProcessNextModules = processModule(module, services.artifactsProvider)
            if (!shouldProcessNextModules) break
        }

        for (handler in allRanHandlers) {
            val wrapperFactory: (Throwable) -> WrappedException = { WrappedException.FromHandler(it, failedModule = null, handler) }
            withAssertionCatching(wrapperFactory) {
                val thereWasAnException = allFailedExceptions.isNotEmpty()
                if (handler.shouldRun(thereWasAnException)) {
                    handler.processAfterAllModules(thereWasAnException)
                }
            }
        }

        if (testConfiguration.metaInfoHandlerEnabled) {
            withAssertionCatching(WrappedException::FromMetaInfoHandler) {
                services.globalMetadataInfoHandler.compareAllMetaDataInfos()
            }
        }

        testConfiguration.afterAnalysisCheckers.forEach {
            withAssertionCatching(WrappedException::FromAfterAnalysisChecker) {
                it.check(allFailedExceptions)
            }
        }
    }

    /*
     * Returns false if next modules should be not processed
     */
    fun processModule(
        module: TestModule,
        artifactsProvider: ArtifactsProvider,
    ): Boolean {
        return runPipelineOnSingleUnit(
            produceStartingArtifact = { testConfiguration.startingArtifactFactory.invoke(module) },
            shouldRunStep = { step, inputArtifact -> step.shouldProcessModule(module, inputArtifact) },
            runStep = { step, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps ->
                step.hackyProcessModule(module, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)
            },
            onArtifactResult = { artifactsProvider.registerArtifact(module, it) },
            onHandlersResult = { step ->
                require(step is TestStep.NonGroupingStep.HandlersStep<*>)
                allRanHandlers += step.handlers
            }
        )
    }

    // -------------------------------------- hacks --------------------------------------

    private fun TestStep.NonGroupingStep<*, *>.hackyProcessModule(
        module: TestModule,
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep.NonGroupingStep<ResultingArtifact.Source, *>)
            .processModule(module, inputArtifact as ResultingArtifact<ResultingArtifact.Source>, thereWereExceptionsOnPreviousSteps)
    }

    private fun <I : ResultingArtifact<I>> TestStep.NonGroupingStep<I, *>.processModule(
        module: TestModule,
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return processModule(module, artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}

class GroupingTestRunner(
    testConfiguration: GroupingPhaseTestConfiguration
) : TestRunner<TestStep.GroupingPhaseStep<*, *>, GroupingPhaseTestConfiguration>(testConfiguration) {
    init {
        testServices.register(TestModuleStructure::class, EmptyModuleStructure)
    }

    fun run(nonGroupingPhaseOutputs: List<NonGroupingPhaseOutput>) {
        testServices.register(GroupingPhaseInputsHolder::class, GroupingPhaseInputsHolder(nonGroupingPhaseOutputs))
        val merger = GroupingPhaseInputsMerger(testServices, testConfiguration.mergerWorkers)
        runPipelineOnSingleUnit(
            produceStartingArtifact = { merger.merge(nonGroupingPhaseOutputs) },
            shouldRunStep = { _, _ -> true },
            runStep = { step, input, thereWereCriticalExceptionsOnPreviousSteps ->
                step.hackyProcess(input, thereWereCriticalExceptionsOnPreviousSteps)
            },
            onArtifactResult = {},
            onHandlersResult = {}
        )
    }

    private object EmptyModuleStructure : TestModuleStructure() {
        override val modules: List<TestModule>
            get() = emptyList()
        override val allDirectives: RegisteredDirectives
            get() = RegisteredDirectivesImpl(emptyList(), emptyMap(), emptyMap())
        override val originalTestDataFiles: List<File>
            get() = emptyList()
    }

    private fun TestStep.GroupingPhaseStep<*, *>.hackyProcess(
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep.GroupingPhaseStep<GroupingPhaseInputArtifact, *>)
            .process(inputArtifact as ResultingArtifact<GroupingPhaseInputArtifact>, thereWereExceptionsOnPreviousSteps)
    }

    private fun <I : ResultingArtifact<I>> TestStep.GroupingPhaseStep<I, *>.process(
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return this.process(artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}
