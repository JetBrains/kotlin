/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.IOException

class TestRunner(private val testConfiguration: TestConfiguration) {
    private val failedAssertions = mutableListOf<WrappedException>()

    fun runTest(@TestDataFile testDataFileName: String, beforeDispose: (TestConfiguration) -> Unit = {}) {
        try {
            runTestImpl(testDataFileName)
        } finally {
            try {
                testConfiguration.testServices.temporaryDirectoryManager.cleanupTemporaryDirectories()
            } catch (_: IOException) {
                // ignored
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

        val globalMetadataInfoHandler = testConfiguration.testServices.globalMetadataInfoHandler
        globalMetadataInfoHandler.parseExistingMetadataInfosFromAllSources()

        val modules = moduleStructure.modules
        val dependencyProvider = DependencyProviderImpl(services, modules)
        services.registerDependencyProvider(dependencyProvider)
        var failedException: WrappedException? = null
        try {
            for (module in modules) {
                val shouldProcessNextModules = processModule(services, module, dependencyProvider, moduleStructure)
                if (!shouldProcessNextModules) break
            }
        } catch (e: WrappedException) {
            failedException = e
        } catch (e: Exception) {
            throw IllegalStateException("Unexpected exception type. Only WrappedException are expected here", e)
        }

        for (handler in testConfiguration.getAllHandlers()) {
            val wrapperFactory = when (handler) {
                is FrontendOutputHandler -> WrappedException::FromFrontendHandler
                is BackendInputHandler -> WrappedException::FromBackendHandler
                is BinaryArtifactHandler -> WrappedException::FromBinaryHandler
                else -> WrappedException::FromUnknownHandler
            }
            withAssertionCatching(wrapperFactory) {
                val thereWasAnException = failedException != null || failedAssertions.isNotEmpty()
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
        if (failedException != null) {
            failedAssertions.add(0, failedException)
        }

        testConfiguration.afterAnalysisCheckers.forEach {
            withAssertionCatching(WrappedException::FromAfterAnalysisChecker) {
                it.check(failedAssertions)
            }
        }

        val filteredFailedAssertions = filterFailedExceptions(failedAssertions)
        filteredFailedAssertions.firstIsInstanceOrNull<WrappedException.FromFacade>()?.let {
            throw it
        }
        services.assertions.assertAll(filteredFailedAssertions)
    }

    private fun filterFailedExceptions(failedExceptions: List<WrappedException>): List<Throwable> {
        return testConfiguration.afterAnalysisCheckers
            .fold(failedExceptions) { assertions, checker ->
                checker.suppressIfNeeded(assertions)
            }
            .sorted()
            .map { it.cause }
    }

    /*
     * If there was failure from handler with `failureDisablesNextSteps=true` then `processModule`
     *   returns false which indicates that other modules should not be processed
     */
    private fun processModule(
        services: TestServices,
        module: TestModule,
        dependencyProvider: DependencyProviderImpl,
        moduleStructure: TestModuleStructure
    ): Boolean {
        val sourcesArtifact = ResultingArtifact.Source()

        val frontendKind = module.frontendKind
        if (!frontendKind.shouldRunAnalysis) return true

        val frontendArtifacts: ResultingArtifact.FrontendOutput<*> = withExceptionWrapping(WrappedException.FromFacade::Frontend) {
            testConfiguration.getFacade(SourcesKind, frontendKind)
                .transform(module, sourcesArtifact)?.also { dependencyProvider.registerArtifact(module, it) }
                ?: return true
        }
        val frontendHandlers: List<AnalysisHandler<*>> = testConfiguration.getHandlers(frontendKind)
        for (frontendHandler in frontendHandlers) {
            val thereWasAnException = withAssertionCatching(WrappedException::FromFrontendHandler) {
                if (frontendHandler.shouldRun(failedAssertions.isNotEmpty())) {
                    frontendHandler.hackyProcess(module, frontendArtifacts)
                }
            }
            if (thereWasAnException && frontendHandler.failureDisablesNextSteps) return false
        }

        val backendKind = services.backendKindExtractor.backendKind(module.targetBackend)
        if (!backendKind.shouldRunAnalysis) return true

        val backendInputInfo = withExceptionWrapping(WrappedException.FromFacade::Converter) {
            testConfiguration.getFacade(frontendKind, backendKind)
                .hackyTransform(module, frontendArtifacts)?.also { dependencyProvider.registerArtifact(module, it) } ?: return true
        }

        val backendHandlers: List<AnalysisHandler<*>> = testConfiguration.getHandlers(backendKind)
        for (backendHandler in backendHandlers) {
            val thereWasAnException = withAssertionCatching(WrappedException::FromBackendHandler) {
                if (backendHandler.shouldRun(failedAssertions.isNotEmpty())) {
                    backendHandler.hackyProcess(module, backendInputInfo)
                }
            }
            if (thereWasAnException && backendHandler.failureDisablesNextSteps) return false
        }

        for (artifactKind in moduleStructure.getTargetArtifactKinds(module)) {
            if (!artifactKind.shouldRunAnalysis) continue
            val binaryArtifact = withExceptionWrapping(WrappedException.FromFacade::Backend) {
                testConfiguration.getFacade(backendKind, artifactKind)
                    .hackyTransform(module, backendInputInfo)?.also {
                        dependencyProvider.registerArtifact(module, it)
                    } ?: return true
            }

            val binaryHandlers: List<AnalysisHandler<*>> = testConfiguration.getHandlers(artifactKind)
            for (binaryHandler in binaryHandlers) {
                val thereWasAnException = withAssertionCatching(WrappedException::FromBinaryHandler) {
                    if (binaryHandler.shouldRun(failedAssertions.isNotEmpty())) {
                        binaryHandler.hackyProcess(module, binaryArtifact)
                    }
                }
                if (thereWasAnException && binaryHandler.failureDisablesNextSteps) return false
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
            failedAssertions += exceptionWrapper(e)
            true
        }
    }

    private inline fun <R> withExceptionWrapping(exceptionWrapper: (Throwable) -> WrappedException, block: () -> R): R {
        return try {
            block()
        } catch (e: Throwable) {
            throw exceptionWrapper(e)
        }
    }

    private fun AnalysisHandler<*>.shouldRun(thereWasAnException: Boolean): Boolean {
        return !(doNotRunIfThereWerePreviousFailures && thereWasAnException)
    }
}

// ----------------------------------------------------------------------------------------------------------------
/*
 * Those `hackyProcess` methods are needed to hack kotlin type system. In common test case
 *   we have artifact of type ResultingArtifact<*> and handler of type AnalysisHandler<*> and actually
 *   at runtime types under `*` are same (that achieved by grouping handlers and facades by
 *   frontend/backend/artifact kind). But there is no way to tell that to compiler, so I unsafely cast types with `*`
 *   to types with Empty artifacts to make it compile. Since unsafe cast has no effort at runtime, it's safe to use it
 */

private fun AnalysisHandler<*>.hackyProcess(module: TestModule, artifact: ResultingArtifact<*>) {
    @Suppress("UNCHECKED_CAST")
    (this as AnalysisHandler<ResultingArtifact.Source>)
        .processModule(module, artifact as ResultingArtifact<ResultingArtifact.Source>)
}

private fun <A : ResultingArtifact<A>> AnalysisHandler<A>.processModule(module: TestModule, artifact: ResultingArtifact<A>) {
    @Suppress("UNCHECKED_CAST")
    processModule(module, artifact as A)
}

private fun AbstractTestFacade<*, *>.hackyTransform(
    module: TestModule,
    artifact: ResultingArtifact<*>
): ResultingArtifact<*>? {
    @Suppress("UNCHECKED_CAST")
    return (this as AbstractTestFacade<ResultingArtifact.Source, ResultingArtifact.Source>)
        .transform(module, artifact as ResultingArtifact<ResultingArtifact.Source>)
}

private fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> AbstractTestFacade<I, O>.transform(
    module: TestModule,
    inputArtifact: ResultingArtifact<I>
): O? {
    @Suppress("UNCHECKED_CAST")
    return transform(module, inputArtifact as I)
}
