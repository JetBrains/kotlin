/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.IOException

class TestRunner(private val testConfiguration: TestConfiguration) {
    companion object {
        fun AnalysisHandler<*>.shouldRun(thereWasAnException: Boolean): Boolean {
            return !(doNotRunIfThereWerePreviousFailures && thereWasAnException)
        }
    }

    private val allFailedExceptions = mutableListOf<WrappedException>()
    private val allRanHandlers = mutableSetOf<AnalysisHandler<*>>()

    fun runTest(@TestDataFile testDataFileName: String, beforeDispose: (TestConfiguration) -> Unit = {}) {
        try {
            runTestImpl(testDataFileName)
        } finally {
            try {
                testConfiguration.testServices.temporaryDirectoryManager.cleanupTemporaryDirectories()
            } catch (e: IOException) {
                println("Failed to clean temporary directories: ${e.message}\n${e.stackTrace}")
            }
            beforeDispose(testConfiguration)
            Disposer.dispose(testConfiguration.rootDisposable)
        }
    }

    private fun runTestImpl(@TestDataFile testDataFileName: String) {
        val services = testConfiguration.testServices

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
            ).singleOrNull() ?: return
            throw exception
        }


        testConfiguration.metaTestConfigurators.forEach {
            if (it.shouldSkipTest()) return
        }

        runTestPipeline(moduleStructure, services)
    }

    fun runTestPipeline(moduleStructure: TestModuleStructure, services: TestServices) {
        val globalMetadataInfoHandler = testConfiguration.testServices.globalMetadataInfoHandler
        globalMetadataInfoHandler.parseExistingMetadataInfosFromAllSources()

        val modules = moduleStructure.modules
        val dependencyProvider = DependencyProviderImpl(services, modules)
        services.registerDependencyProvider(dependencyProvider)

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            preprocessor.preprocessModuleStructure(moduleStructure)
        }

        testConfiguration.preAnalysisHandlers.forEach { preprocessor ->
            withAssertionCatching(WrappedException::FromPreAnalysisHandler) {
                preprocessor.prepareSealedClassInheritors(moduleStructure)
            }
        }

        for (module in modules) {
            val shouldProcessNextModules = processModule(module, dependencyProvider)
            if (!shouldProcessNextModules) break
        }

        for (handler in allRanHandlers) {
            val wrapperFactory: (Throwable) -> WrappedException = { WrappedException.FromHandler(it, handler) }
            withAssertionCatching(wrapperFactory) {
                val thereWasAnException = allFailedExceptions.isNotEmpty()
                if (handler.shouldRun(thereWasAnException)) {
                    handler.processAfterAllModules(thereWasAnException)
                }
            }
        }
        if (testConfiguration.metaInfoHandlerEnabled) {
            withAssertionCatching(WrappedException::FromMetaInfoHandler) {
                globalMetadataInfoHandler.compareAllMetaDataInfos()
            }
        }

        testConfiguration.afterAnalysisCheckers.forEach {
            withAssertionCatching(WrappedException::FromAfterAnalysisChecker) {
                it.check(allFailedExceptions)
            }
        }

        reportFailures(services)
    }

    fun reportFailures(services: TestServices) {
        val filteredFailedAssertions = filterFailedExceptions(allFailedExceptions)
        filteredFailedAssertions.firstIsInstanceOrNull<WrappedException.FromFacade>()?.let {
            throw it
        }
        services.assertions.failAll(filteredFailedAssertions)
    }

    /*
     * Returns false if next modules should be not processed
     */
    fun processModule(
        module: TestModule,
        dependencyProvider: DependencyProviderImpl
    ): Boolean {
        var inputArtifact = testConfiguration.startingArtifactFactory.invoke(module)

        for (step in testConfiguration.steps) {
            if (!step.shouldProcessModule(module, inputArtifact)) continue

            val thereWereCriticalExceptionsOnPreviousSteps = allFailedExceptions.any { it.failureDisablesNextSteps }
            when (val result = step.hackyProcessModule(module, inputArtifact, thereWereCriticalExceptionsOnPreviousSteps)) {
                is TestStep.StepResult.Artifact<*> -> {
                    require(step is TestStep.FacadeStep<*, *>)
                    if (step.inputArtifactKind != step.outputArtifactKind) {
                        dependencyProvider.registerArtifact(module, result.outputArtifact)
                    }
                    inputArtifact = result.outputArtifact
                }
                is TestStep.StepResult.ErrorFromFacade -> {
                    allFailedExceptions += result.exception
                    return false
                }
                is TestStep.StepResult.HandlersResult -> {
                    val (exceptionsFromHandlers, shouldRunNextSteps) = result
                    require(step is TestStep.HandlersStep<*>)
                    allRanHandlers += step.handlers
                    allFailedExceptions += exceptionsFromHandlers
                    if (!shouldRunNextSteps) {
                        return false
                    }
                }
                is TestStep.StepResult.NoArtifactFromFacade -> return false
            }
        }
        return true
    }

    /*
     * Returns true if there was an exception in block
     */
    private inline fun withAssertionCatching(exceptionWrapper: (Throwable) -> WrappedException, block: () -> Unit): Boolean {
        return try {
            block()
            false
        } catch (e: Throwable) {
            allFailedExceptions += exceptionWrapper(e)
            true
        }
    }

    private fun filterFailedExceptions(failedExceptions: List<WrappedException>): List<Throwable> {
        return testConfiguration.afterAnalysisCheckers
            .fold(failedExceptions) { assertions, checker ->
                checker.suppressIfNeeded(assertions)
            }
            .sorted()
            .map { it.cause }
    }

    // -------------------------------------- hacks --------------------------------------

    private fun TestStep<*, *>.hackyProcessModule(
        module: TestModule,
        inputArtifact: ResultingArtifact<*>,
        thereWereExceptionsOnPreviousSteps: Boolean
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return (this as TestStep<ResultingArtifact.Source, *>)
            .processModule(module, inputArtifact as ResultingArtifact<ResultingArtifact.Source>, thereWereExceptionsOnPreviousSteps)
    }

    private fun <I : ResultingArtifact<I>> TestStep<I, *>.processModule(
        module: TestModule,
        artifact: ResultingArtifact<I>,
        thereWereExceptionsOnPreviousSteps: Boolean
    ): TestStep.StepResult<*> {
        @Suppress("UNCHECKED_CAST")
        return processModule(module, artifact as I, thereWereExceptionsOnPreviousSteps)
    }
}
