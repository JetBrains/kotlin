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
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.IOException

sealed class TestRunner<Step : TestStep<*, *>, Configuration : TestConfiguration<Step>>(val testConfiguration: Configuration) {
    val testServices: TestServices get() = testConfiguration.testServices
    val failuresInterceptor = FailuresInterceptor(testConfiguration)

    open fun runTestPreprocessing() {
        testServices.registerArtifactsProvider(ArtifactsProvider())

        val moduleStructure = testServices.moduleStructure
        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            preprocessor.preprocessModuleStructure(moduleStructure)
        }

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            failuresInterceptor.withAssertionCatching(WrappedException::FromPreAnalysisHandler) {
                preprocessor.prepareSealedClassInheritors(moduleStructure)
            }
        }
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

    protected sealed class StepInput {
        /** The step should be executed with [inputArtifact]. */
        class Run(val inputArtifact: ResultingArtifact<*>) : StepInput()

        /** The step is not applicable in this pipeline run (e.g. the branch producing its input artifact was not taken). */
        object Skip : StepInput()

        /** The pipeline is configured incorrectly, so the whole unit processing should be aborted with [exception]. */
        class Fail(val exception: WrappedException) : StepInput()
    }

    /**
     * Runs all configured [TestConfiguration.steps] one after another, tracking the artifact produced by the last
     * executed facade step (the *latest* artifact).
     *
     * [resolveStepInput] decides for each step whether it should be executed, skipped or reported as a
     * misconfiguration, and which artifact should be passed to it.
     */
    protected fun runPipelineOnSingleUnit(
        produceStartingArtifact: () -> ResultingArtifact<*>,
        resolveStepInput: (Step, ResultingArtifact<*>) -> StepInput,
        runStep: RunStep<Step>,
        onArtifactResult: (ResultingArtifact<*>) -> Unit,
        onHandlersResult: (Step) -> Unit
    ): Boolean {
        var latestArtifact = produceStartingArtifact()

        for (step in testConfiguration.steps) {
            val inputArtifact = when (val stepInput = resolveStepInput(step, latestArtifact)) {
                is StepInput.Skip -> continue
                is StepInput.Fail -> {
                    @OptIn(PrivateForInline::class)
                    failuresInterceptor._allFailedExceptions += stepInput.exception
                    return false
                }
                is StepInput.Run -> stepInput.inputArtifact
            }

            val thereWereCriticalExceptionsOnPreviousSteps = failuresInterceptor.allFailedExceptions.any { it.failureDisablesNextSteps }
            when (val result = runStep.run(step, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)) {
                is TestStep.StepResult.Artifact<*> -> {
                    checkTestInfrastructure(step is TestStep.FacadeStep<*, *>) { "Step must be FacadeStep" }
                    onArtifactResult(result.outputArtifact)
                    latestArtifact = result.outputArtifact
                }
                is TestStep.StepResult.ErrorFromFacade -> {
                    @OptIn(PrivateForInline::class)
                    failuresInterceptor._allFailedExceptions += result.exception
                    return false
                }
                is TestStep.StepResult.HandlersResult -> {
                    val (exceptionsFromHandlers, shouldRunNextSteps) = result
                    @OptIn(PrivateForInline::class)
                    failuresInterceptor._allFailedExceptions += exceptionsFromHandlers
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

    protected fun renderPipeline(currentStep: Step): String {
        return testConfiguration.steps.joinToString(separator = "\n", prefix = "Configured pipeline:\n") { step ->
            val output = step.outputArtifactKind?.let { " -> $it" }.orEmpty()
            val suffix = if (step == currentStep) " <---------------------------" else ""
            "  ${step.inputArtifactKind}$output: $step$suffix"
        }
    }

    class FailuresInterceptor(val testConfiguration: TestConfiguration<*>) {
        @OptIn(PrivateForInline::class)
        val allFailedExceptions: List<WrappedException> get() = _allFailedExceptions

        @Suppress("PropertyName")
        @PrivateForInline
        @PublishedApi
        internal val _allFailedExceptions: MutableList<WrappedException> = mutableListOf()

        val hasFailures: Boolean get() = allFailedExceptions.isNotEmpty()

        /**
         * @return true if there were any failures from any steps, even if they were suppressed by [FailuresInterceptor]s
         */
        fun reportFailures(checkForUnmuting: Boolean): Boolean {
            val filteredFailedAssertions = when {
                hasFailures -> filterFailedExceptions(allFailedExceptions)
                checkForUnmuting -> {
                    for (suppressor in testConfiguration.failureSuppressors) {
                        withAssertionCatching(WrappedException::FromFailingTestSuppressor) {
                            suppressor.checkIfTestShouldBeUnmuted()
                        }
                    }
                    allFailedExceptions.map { it.cause }
                }
                else -> emptyList()
            }
            filteredFailedAssertions.firstIsInstanceOrNull<WrappedException.FromFacade>()?.let {
                throw it
            }
            testConfiguration.testServices.assertions.failAll(filteredFailedAssertions)
            return hasFailures
        }

        /*
         * Returns true if there was an exception in block
         */
        inline fun withAssertionCatching(exceptionWrapper: (Throwable) -> WrappedException, block: () -> Unit): Boolean {
            return try {
                block()
                false
            } catch (e: Throwable) {
                @OptIn(PrivateForInline::class)
                testConfiguration.testServices.assertions.unfoldException(e).mapTo(_allFailedExceptions) { exceptionWrapper(it) }
                true
            }
        }

        fun filterFailedExceptions(failedExceptions: List<WrappedException>): List<Throwable> {
            // Failures coming from the test infrastructure itself must never be suppressed (e.g. by IGNORE_BACKEND),
            // otherwise an infra problem would be masked as a green test, hiding the real (unknown) test status.
            val [infrastructureFailures, suppressableFailures] = failedExceptions.partition { it.isTestInfrastructureFailure }
            val notSuppressedFailures = testConfiguration.failureSuppressors
                .fold(suppressableFailures) { assertions, suppressor ->
                    if (assertions.isEmpty()) return@fold assertions
                    suppressor.suppressIfNeeded(assertions)
                }
            return (infrastructureFailures + notSuppressedFailures)
                .sorted()
                .map { it.cause }
        }

        operator fun plusAssign(other: FailuresInterceptor) {
            @OptIn(PrivateForInline::class)
            _allFailedExceptions += other.allFailedExceptions
        }
    }
}

class NonGroupingTestRunner(
    testConfiguration: NonGroupingStageTestConfiguration
) : TestRunner<TestStep.NonGroupingStep<*, *>, NonGroupingStageTestConfiguration>(testConfiguration) {
    companion object {
        fun AnalysisHandler<*>.shouldRun(thereWasAnException: Boolean): Boolean {
            return !(doNotRunIfThereWerePreviousFailures && thereWasAnException)
        }
    }

    private val allRanHandlers = mutableSetOf<AnalysisHandler<*>>()

    fun runTest(@TestDataFile testDataFileName: String, beforeDispose: (NonGroupingStageTestConfiguration) -> Unit = {}) {
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
            val exception = failuresInterceptor.filterFailedExceptions(
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
        failuresInterceptor.reportFailures(checkForUnmuting = true)
    }

    override fun runTestPreprocessing() {
        super.runTestPreprocessing()
        val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
        globalMetadataInfoHandler.parseExistingMetadataInfosFromAllSources()
    }

    fun runSteps() {
        val services = testConfiguration.testServices
        val moduleStructure = services.moduleStructure

        val firstModule = moduleStructure.modules.firstOrNull()
        if (firstModule != null) {
            val startingArtifactKind = testConfiguration.startingArtifactFactory.invoke(firstModule).kind
            val pipelineIsInvalid = failuresInterceptor.withAssertionCatching({ WrappedException.FromTestPipeline(it, failedModule = null) }) {
                validatePipeline(startingArtifactKind)
            }
            if (pipelineIsInvalid) return
        }

        for (module in moduleStructure.modules) {
            val shouldProcessNextModules = processModule(module, services.artifactsProvider)
            if (!shouldProcessNextModules) break
        }

        for (handler in allRanHandlers) {
            val wrapperFactory: (Throwable) -> WrappedException = { WrappedException.FromHandler(it, failedModule = null, handler) }
            failuresInterceptor.withAssertionCatching(wrapperFactory) {
                val thereWasAnException = failuresInterceptor.hasFailures
                if (handler.shouldRun(thereWasAnException)) {
                    handler.processAfterAllModules(thereWasAnException)
                }
            }
        }

        if (testConfiguration.metaInfoHandlerEnabled) {
            failuresInterceptor.withAssertionCatching(WrappedException::FromMetaInfoHandler) {
                services.globalMetadataInfoHandler.compareAllMetaDataInfos()
            }
        }

        testConfiguration.afterAnalysisCheckers.forEach {
            failuresInterceptor.withAssertionCatching(WrappedException::FromAfterAnalysisChecker) {
                it.check(thereWereFailures = failuresInterceptor.hasFailures)
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
            produceStartingArtifact = {
                testConfiguration.startingArtifactFactory.invoke(module).also {
                    testServices.artifactsProvider.registerArtifact(module, it)
                }
            },
            resolveStepInput = { step, latestArtifact ->
                resolveStepInput(step, latestArtifact, module, artifactsProvider)
            },
            runStep = { step, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps ->
                step.hackyProcessModule(module, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)
            },
            onArtifactResult = {
                artifactsProvider.registerArtifact(module, it)
            },
            onHandlersResult = { step ->
                checkTestInfrastructure(step is TestStep.NonGroupingStep.HandlersStep<*>) { "Step must be HandlersStep" }
                allRanHandlers += step.handlers
            }
        )
    }

    /**
     * Decides how [step] should be handled for [module], based on the [latestArtifact] produced by the pipeline so far
     * and on all artifacts produced for this module earlier ([artifactsProvider]).
     */
    private fun resolveStepInput(
        step: TestStep.NonGroupingStep<*, *>,
        latestArtifact: ResultingArtifact<*>,
        module: TestModule,
        artifactsProvider: ArtifactsProvider,
    ): StepInput {
        val inputKind = step.inputArtifactKind
        return when (step) {
            is TestStep.NonGroupingStep.FacadeStep<*, *> -> {
                val inputArtifact = artifactsProvider.getArtifactSafe(module, inputKind)
                when {
                    // There is no suitable input artifact, or the facade doesn't want to process this module. In both
                    // cases the facade produces nothing, so all steps depending on its output should be skipped too.
                    inputArtifact == null || !step.facade.shouldTransform(module) -> StepInput.Skip
                    else -> StepInput.Run(inputArtifact)
                }
            }

            is TestStep.NonGroupingStep.HandlersStep<*> -> when {
                latestArtifact.kind == inputKind -> StepInput.Run(latestArtifact)
                artifactsProvider.getArtifactSafe(module, inputKind) != null -> {
                    val message = """
                        Incorrect test configuration: handlers step "$step" is declared after a facade which replaced
                        the artifact of kind $inputKind with an artifact of kind ${latestArtifact.kind}.
                        Handlers steps always run on the latest produced artifact, so this step should be moved right
                        after the facade producing $inputKind.
                    """.trimIndent() + "\n" + renderPipeline(step)
                    StepInput.Fail(WrappedException.FromTestPipeline(TestInfrastructureException(message), module))
                }
                // No artifact of the required kind was ever produced for this module: the pipeline never reached the
                // facade which produces it.
                else -> StepInput.Skip
            }
        }
    }

    private fun validatePipeline(startingArtifactKind: TestArtifactKind<*>) {
        val producibleArtifactKinds = mutableSetOf(startingArtifactKind)
        for (step in testConfiguration.steps) {
            checkTestInfrastructure(step.inputArtifactKind in producibleArtifactKinds) {
                "Incorrect test configuration: step \"$step\" consumes an artifact of kind ${step.inputArtifactKind}, " +
                        "but none of the preceding steps produces it.\n" +
                        renderPipeline(step)
            }
            step.outputArtifactKind?.let { producibleArtifactKinds += it }
        }
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
    testConfiguration: GroupingStageTestConfiguration
) : TestRunner<TestStep.GroupingStageStep<*, *>, GroupingStageTestConfiguration>(testConfiguration) {
    init {
        testServices.register(TestModuleStructure::class, EmptyModuleStructure)
    }

    fun run(nonGroupingStageOutputs: List<NonGroupingStageOutput>) {
        testServices.register(GroupingStageInputsHolder::class, GroupingStageInputsHolder(nonGroupingStageOutputs))
        val merger = GroupingStageInputsMerger(testServices, testConfiguration.mergerWorkers)
        runPipelineOnSingleUnit(
            produceStartingArtifact = { merger.merge(nonGroupingStageOutputs) },
            resolveStepInput = ::resolveStepInput,
            runStep = { step, input, thereWereCriticalExceptionsOnPreviousSteps ->
                step.hackyProcess(input, thereWereCriticalExceptionsOnPreviousSteps)
            },
            onArtifactResult = {},
            onHandlersResult = {}
        )
    }

    private fun resolveStepInput(
        step: TestStep.GroupingStageStep<*, *>,
        latestArtifact: ResultingArtifact<*>,
    ): StepInput {
        if (latestArtifact.kind == step.inputArtifactKind) return StepInput.Run(latestArtifact)
        val message = """
            Incorrect test configuration: step "$step" of the grouping stage expects an artifact
            of kind ${step.inputArtifactKind}, but the latest produced artifact has kind ${latestArtifact.kind}.
            The grouping stage pipeline is strictly linear, so each step must consume
            the artifact produced by the previous one.
        """.trimIndent() + "\n" + renderPipeline(step)
        return StepInput.Fail(WrappedException.FromTestPipeline(TestInfrastructureException(message), failedModule = null))
    }

    private object EmptyModuleStructure : TestModuleStructure() {
        override val modules: List<TestModule>
            get() = emptyList()
        override val allDirectives: RegisteredDirectives
            get() = RegisteredDirectivesImpl(emptyList(), emptyMap(), emptyMap())
        override val originalTestDataFiles: List<File>
            get() = emptyList()
    }

    private fun TestStep.GroupingStageStep<*, *>.hackyProcess(
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep.GroupingStageStep<GroupingStageInputArtifact, *>)
            .process(inputArtifact as ResultingArtifact<GroupingStageInputArtifact>, thereWereExceptionsOnPreviousSteps)
    }

    private fun <I : ResultingArtifact<I>> TestStep.GroupingStageStep<I, *>.process(
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean,
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return this.process(artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}
