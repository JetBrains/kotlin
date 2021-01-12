/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class TestRunner(private val testConfiguration: TestConfiguration) {
    private val failedAssertions = mutableListOf<AssertionError>()

    fun runTest(@TestDataFile testDataFileName: String) {
        try {
            runTestImpl(testDataFileName)
        } finally {
            Disposer.dispose(testConfiguration.rootDisposable)
        }
    }

    private fun runTestImpl(@TestDataFile testDataFileName: String) {
        val services = testConfiguration.testServices

        @Suppress("NAME_SHADOWING")
        val testDataFileName = testConfiguration.metaTestConfigurators.fold(testDataFileName) { fileName, configurator ->
            configurator.transformTestDataPath(fileName)
        }

        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            testDataFileName,
            testConfiguration.directives,
        ).also {
            services.register(TestModuleStructure::class, it)
        }

        testConfiguration.metaTestConfigurators.forEach {
            if (it.shouldSkipTest()) return
        }

        val globalMetadataInfoHandler = testConfiguration.testServices.globalMetadataInfoHandler
        globalMetadataInfoHandler.parseExistingMetadataInfosFromAllSources()

        val modules = moduleStructure.modules
        val dependencyProvider = DependencyProviderImpl(services, modules)
        services.registerDependencyProvider(dependencyProvider)
        var failedException: Throwable? = null
        try {
            for (module in modules) {
                processModule(module, dependencyProvider, moduleStructure)
            }
        } catch (e: Throwable) {
            failedException = e
        }
        for (handler in testConfiguration.getAllHandlers()) {
            withAssertionCatching { handler.processAfterAllModules(failedAssertions.isNotEmpty()) }
        }
        if (testConfiguration.metaInfoHandlerEnabled) {
            withAssertionCatching(insertExceptionInStart = true) {
                globalMetadataInfoHandler.compareAllMetaDataInfos()
            }
        }
        if (failedException != null) {
            failedAssertions.add(0, ExceptionFromTestError(failedException))
        }

        testConfiguration.afterAnalysisCheckers.forEach {
            withAssertionCatching {
                it.check(failedAssertions)
            }
        }

        val filteredFailedAssertions = testConfiguration.afterAnalysisCheckers
            .fold<AfterAnalysisChecker, List<AssertionError>>(failedAssertions) { assertions, checker ->
                checker.suppressIfNeeded(assertions)
            }

        services.assertions.assertAll(filteredFailedAssertions)
    }

    private fun processModule(
        module: TestModule,
        dependencyProvider: DependencyProviderImpl,
        moduleStructure: TestModuleStructure
    ) {
        val sourcesArtifact = ResultingArtifact.Source()

        val frontendKind = module.frontendKind
        if (!frontendKind.shouldRunAnalysis) return

        val frontendArtifacts: ResultingArtifact.FrontendOutput<*> = testConfiguration.getFacade(SourcesKind, frontendKind)
            .transform(module, sourcesArtifact).also { dependencyProvider.registerArtifact(module, it) }
        val frontendHandlers: List<AnalysisHandler<*>> = testConfiguration.getHandlers(frontendKind)
        for (frontendHandler in frontendHandlers) {
            withAssertionCatching {
                frontendHandler.hackyProcess(module, frontendArtifacts)
            }
        }

        val backendKind = module.backendKind
        if (!backendKind.shouldRunAnalysis) return

        val backendInputInfo = testConfiguration.getFacade(frontendKind, backendKind)
            .hackyTransform(module, frontendArtifacts).also { dependencyProvider.registerArtifact(module, it) }

        val backendHandlers: List<AnalysisHandler<*>> = testConfiguration.getHandlers(backendKind)
        for (backendHandler in backendHandlers) {
            withAssertionCatching { backendHandler.hackyProcess(module, backendInputInfo) }
        }

        for (artifactKind in moduleStructure.getTargetArtifactKinds(module)) {
            if (!artifactKind.shouldRunAnalysis) continue
            val binaryArtifact = testConfiguration.getFacade(backendKind, artifactKind)
                .hackyTransform(module, backendInputInfo).also {
                    dependencyProvider.registerArtifact(module, it)
                }

            val binaryHandlers: List<AnalysisHandler<*>> = testConfiguration.getHandlers(artifactKind)
            for (binaryHandler in binaryHandlers) {
                withAssertionCatching { binaryHandler.hackyProcess(module, binaryArtifact) }
            }
        }
    }

    private inline fun withAssertionCatching(insertExceptionInStart: Boolean = false, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            if (insertExceptionInStart) {
                failedAssertions.add(0, e)
            } else {
                failedAssertions += e
            }
        }
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
): ResultingArtifact<*> {
    @Suppress("UNCHECKED_CAST")
    return (this as AbstractTestFacade<ResultingArtifact.Source, ResultingArtifact.Source>)
        .transform(module, artifact as ResultingArtifact<ResultingArtifact.Source>)
}

private fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> AbstractTestFacade<I, O>.transform(
    module: TestModule,
    inputArtifact: ResultingArtifact<I>
): O {
    @Suppress("UNCHECKED_CAST")
    return transform(module, inputArtifact as I)
}
