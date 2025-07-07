/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLStubBasedTest.Companion.computeAstLoadingAware
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesByStubTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test case allows testing differences between stub-based and AST-based files.
 *
 * The entry point for tests is [doTest].
 *
 * @see computeAstLoadingAware
 * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
 * @see com.intellij.psi.impl.PsiManagerEx.isAssertOnFileLoading
 */
abstract class AbstractLLStubBasedTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

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

        val stubBasedFile = AbstractRawFirBuilderLazyBodiesByStubTest.createKtFile(mainFile, disposable)
        doTest(
            astBasedFile = mainFile,
            stubBasedFile = stubBasedFile,
            mainModule = mainModule,
            testServices = testServices,
        )
    }

    abstract fun doTest(
        astBasedFile: KtFile,
        stubBasedFile: KtFile,
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
