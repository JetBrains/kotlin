/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLAnnotationArgumentsCalculatorTest.AnnotationResult
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase.Companion.collectAnnotations
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import java.util.*

/**
 * This test case ensures that annotation arguments don't require AST loading for performance reasons.
 *
 * Usually it is expected to see AST loading only in the case of body access which happens at latest resolution phases.
 *
 * Initial issue: [KT-71787](https://youtrack.jetbrains.com/issue/KT-71787)
 */
abstract class AbstractLLAnnotationArgumentsCalculatorTest : AbstractLLStubBasedTest<List<AnnotationResult>>() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        val STUB_DIFFERENCE by directive("Indicates that stub-based and AST-based annotations differ")
    }

    context(facade: LLResolutionFacade)
    override fun doStubBasedTest(stubBasedFile: KtFile, mainModule: KtTestModule, testServices: TestServices): List<AnnotationResult> {
        return collectStubBasedAndAssertAstBasedAnnotations(stubBasedFile, testServices)
    }

    context(facade: LLResolutionFacade)
    override fun doAstBasedValidation(
        stubBasedOutput: List<@JvmWildcard AnnotationResult>,
        astBasedFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices
    ) {
        val astBasedAnnotations = collectAnnotations(astBasedFile)
        testServices.assertConsistency(
            astBasedAnnotations = astBasedAnnotations,
            stubBasedAnnotations = stubBasedOutput,
        )
    }

    private fun TestServices.assertConsistency(astBasedAnnotations: List<AnnotationResult>, stubBasedAnnotations: List<AnnotationResult>) {
        val stubDifferenceExpected = Directives.STUB_DIFFERENCE in moduleStructure.allDirectives
        val differentStubBasedAnnotations = mutableListOf<AnnotationResult>()

        for (stubBasedAnnotation in stubBasedAnnotations) {
            val astBasedAnnotation = astBasedAnnotations[stubBasedAnnotation.globalIndex]
            if (astBasedAnnotation == stubBasedAnnotation) continue

            if (stubDifferenceExpected) {
                differentStubBasedAnnotations += stubBasedAnnotation
            } else {
                assertions.assertEquals(expected = astBasedAnnotation, actual = stubBasedAnnotation) {
                    "AST-based annotation and Stub-based annotation differ.\n" +
                            "'// ${Directives.STUB_DIFFERENCE}' can be used to suppress the exception if such differences are expected."
                }
            }
        }

        val expectedFile = getTestOutputFile(".stubBasedAnnotations.txt").toFile()
        if (!stubDifferenceExpected) {
            assertions.assertFileDoesntExist(expectedFile) {
                "No failed annotations, but ${expectedFile.name} exists"
            }

            return
        }

        if (differentStubBasedAnnotations.isEmpty()) {
            assertions.fail {
                "'// ${Directives.STUB_DIFFERENCE}' directive is unused and has to be dropped"
            }
        }

        val actualText = differentStubBasedAnnotations.joinToString(separator = "\n----------\n\n") { stubBasedAnnotation ->
            val astBasedAnnotation = astBasedAnnotations[stubBasedAnnotation.globalIndex]
            prettyPrint {
                appendLine("Expected:")
                withIndent {
                    appendLine(astBasedAnnotation.toString())
                }

                appendLine("Actual:")
                withIndent {
                    appendLine(stubBasedAnnotation.toString())
                }
            }
        }

        assertions.assertEqualsToFile(expectedFile, actualText)
    }

    class AnnotationResult(
        val globalIndex: Int,
        val annotation: String,
        val isCalculatedSuccessfully: Boolean,
        val context: String,
    ) {
        override fun toString(): String = buildString {
            append("context -> ")
            appendLine(context)
            append(annotation)
        }

        override fun equals(other: Any?): Boolean = this === other ||
                other is AnnotationResult &&
                other.globalIndex == globalIndex &&
                other.context == context &&
                other.annotation == annotation

        override fun hashCode(): Int = Objects.hash(globalIndex, context, annotation)
    }

    context(facade: LLResolutionFacade)
    private fun collectAnnotations(file: KtFile): List<AnnotationResult> {
        val firFile = file.getOrBuildFirFile(facade)
        val annotations = firFile.collectAnnotations()
        return annotations.mapIndexed { index, annotationWithContext ->
            val annotationCall = annotationWithContext.annotation
            val isCalculatedSuccessfully = computeAstLoadingAware {
                FirLazyBodiesCalculator.calculateAnnotation(annotationCall, facade.useSiteFirSession)
            } != null

            AnnotationResult(
                globalIndex = index,
                annotation = annotationCall.render().trim(),
                isCalculatedSuccessfully = isCalculatedSuccessfully,
                context = annotationWithContext.context,
            )
        }
    }

    context(facade: LLResolutionFacade)
    private fun collectStubBasedAndAssertAstBasedAnnotations(stubBasedFile: KtFile, testServices: TestServices): List<AnnotationResult> {
        // We analyze the stub-based file, so all failed annotations represent annotations which
        // weren't able to calculate arguments via stubs
        val (stubBasedAnnotations, astBasedAnnotations) = collectAnnotations(stubBasedFile).partition {
            it.isCalculatedSuccessfully
        }

        testServices.assertAstBasedAnnotations(astBasedAnnotations)
        return stubBasedAnnotations
    }

    private fun TestServices.assertAstBasedAnnotations(annotations: List<AnnotationResult>) {
        val expectedFile = getTestOutputFile(".astBasedAnnotations.txt").toFile()
        if (annotations.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile) {
                "No failed annotations, but ${expectedFile.name} exists"
            }

            return
        }

        val actualText = buildString {
            appendLine(
                """
                Annotations from the list below require AST loading to calculate arguments.
                It is expected for invalid code, but valid arguments should be calculated via stubs for performance reasons.
                See KT-71787 for reference.
                
                """.trimIndent(),
            )

            annotations.joinTo(this, separator = "\n\n")
        }

        assertions.assertEqualsToFile(expectedFile, actualText)
    }
}

abstract class AbstractLLSourceAnnotationArgumentsCalculatorTest : AbstractLLAnnotationArgumentsCalculatorTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}
