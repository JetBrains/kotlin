/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLStubBasedTest.Companion.computeAstLoadingAware
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesByStubTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test case allows testing differences between stub-based and AST-based files.
 *
 * The entry points for tests are [doStubBasedTest] and [doAstBasedValidation].
 *
 * @see withAstLoadingAssertion
 * @see computeAstLoadingAware
 * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
 * @see com.intellij.psi.impl.PsiManagerEx.isAssertOnFileLoading
 */
abstract class AbstractLLStubBasedTest<StubBasedOutput> : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)

        builder.defaultDirectives {
            +AnalysisApiTestDirectives.STUB_BASED
        }
    }

    protected object Directives : SimpleDirectivesContainer() {
        /**
         * This directive has to be in sync with [AbstractRawFirBuilderLazyBodiesByStubTest]
         * as [AbstractLLAnnotationArgumentsCalculatorTest] is supposed to work as an extension to the stub test.
         */
        val IGNORE_TREE_ACCESS by stringDirective("Disables the test. The YT issue number has to be provided")

        val INCONSISTENT_DECLARATIONS by directive("Indicates that stub-based and AST-based have a different number of declarations")
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        if (Directives.IGNORE_TREE_ACCESS in testServices.moduleStructure.allDirectives) return

        // The main file is already put into a stub-based state by the base test via the `STUB_BASED` default directive (see `configureTest`).
        val output = withAstLoadingAssertion(mainFile) {
            withResolutionFacade(mainFile) { facade ->
                doStubBasedTest(mainFile, mainModule, testServices, facade = facade)
            }
        }

        // We need to clear caches to ensure that we will not load the file from the cache
        clearCaches(mainFile.project)
        mainFile.calcTreeElement()
        testServices.assertions.assertTrue(mainFile.stub == null) { "Stub shouldn't be present for loaded file" }

        withResolutionFacade(mainFile) { facade ->
            doAstBasedValidation(output, mainFile, mainModule, testServices, facade = facade)
        }
    }

    /**
     * The main logic of the test.
     * [stubBasedFile] has no loaded AST tree and throws exceptions on its access.
     * [computeAstLoadingAware] should be used to guard the test logic against such exceptions.
     *
     * @see computeAstLoadingAware
     * @see doAstBasedValidation
     */
    context(facade: LLResolutionFacade)
    abstract fun doStubBasedTest(
        stubBasedFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): StubBasedOutput

    /**
     * Performs additional validation of the [doStubBasedTest] output.
     * [astBasedFile] has loaded AST tree and no stubs.
     *
     * This function is supposed to repeat the same amount of work as [doStubBasedTest] does,
     * but on top of the AST tree instead of the stubs, and compare the results.
     *
     * The project has no cached values from [doStubBasedTest] step, so it can re-resolve the file.
     *
     * @param stubBasedOutput the output of [doStubBasedTest]
     * */
    context(facade: LLResolutionFacade)
    abstract fun doAstBasedValidation(
        stubBasedOutput: StubBasedOutput,
        astBasedFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    )

    companion object {
        /**
         * Runs the given [action] and returns its result.
         * `null` is returned in case the ast loading attempt.
         */
        fun <T : Any> computeAstLoadingAware(action: () -> T): T? {
            val result = runCatching {
                action()
            }

            result.exceptionOrNull()?.let { exception ->
                if (exception.message?.startsWith("Access to tree elements not allowed for") != true) {
                    throw exception
                }
            }

            return result.getOrNull()
        }
    }
}
