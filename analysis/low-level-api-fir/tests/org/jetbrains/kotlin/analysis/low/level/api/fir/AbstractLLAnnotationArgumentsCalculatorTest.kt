/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesByStubTest
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase.Companion.collectAnnotations
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test case ensures that annotation arguments don't require AST loading for performance reasons.
 *
 * Usually it is expected to see AST loading only in the case of body access which happens at latest resolution phases.
 *
 * Initial issue: [KT-71787](https://youtrack.jetbrains.com/issue/KT-71787)
 */
abstract class AbstractLLAnnotationArgumentsCalculatorTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        /**
         * This directive has to be in sync with [AbstractRawFirBuilderLazyBodiesByStubTest]
         * as [AbstractLLAnnotationArgumentsCalculatorTest] is supposed to work as an extension to the stub test.
         */
        val IGNORE_TREE_ACCESS by stringDirective("Disables the test. The YT issue number has to be provided")
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        if (Directives.IGNORE_TREE_ACCESS in testServices.moduleStructure.allDirectives) return

        val file = AbstractRawFirBuilderLazyBodiesByStubTest.createKtFile(mainFile, disposable)
        withResolutionFacade(file) { resolutionFacade ->
            val firFile = file.getOrBuildFirFile(resolutionFacade)
            val annotations = firFile.collectAnnotations()
            val failedAnnotations = annotations.filter {
                val result = this.runCatching { FirLazyBodiesCalculator.calculateAnnotation(it.annotation, firFile.moduleData.session) }
                result.exceptionOrNull()?.let { exception ->
                    if (exception.message?.startsWith("Access to tree elements not allowed for") != true) {
                        throw exception
                    }
                }

                result.isFailure
            }

            val expectedFile = getTestOutputFile(".astBasedAnnotations.txt").toFile()
            if (failedAnnotations.isEmpty()) {
                testServices.assertions.assertFileDoesntExist(expectedFile) {
                    "No failed annotations, but ${expectedFile.name} exists"
                }
            } else {
                val actualText = buildString {
                    this.appendLine(
                        """
                        Annotations from the list below require AST loading to calculate arguments.
                        It is expected for invalid code, but valid arguments should be calculated via stubs for performance reasons.
                        See KT-71787 for reference.
                        
                        """.trimIndent()
                    )

                    failedAnnotations.joinTo(this, separator = "\n") { context ->
                        val annotationCall = context.annotation
                        buildString {
                            this.appendLine("context -> ${context.context}")
                            this.appendLine(annotationCall.render().trim())
                        }
                    }
                }

                testServices.assertions.assertEqualsToFile(expectedFile, actualText)
            }
        }
    }
}

abstract class AbstractLLSourceAnnotationArgumentsCalculatorTest : AbstractLLAnnotationArgumentsCalculatorTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}
