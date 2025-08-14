/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaCapturedTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

/**
 * Test for type building DSL from [org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator].
 *
 * The test reuses the regular test infrastructure to retrieve types from expressions in the test data.
 * The test builds a `caret -> type` mapping from the test data and then runs a corresponding DSL test from [TestCases].
 * Each `caret` in the test file should mark a `KtExpression`, return type of which should be used in the type construction.
 *
 * If the test file is named `A.kt` and is placed inside `analysis/analysis-api/testData/types/typeCreation/byDsl/classType`,
 * then there should be a function called `testA` in the `ClassTypeCreatorDslTestCases` subclass of [TestCases],
 * which retrieves types from [TestCases.caretToType] mapping and returns some value constructed from them using the type-building DSL.
 * The returned value is then rendered in the output file `A.txt`. The mapping from directories (`/classType`) to the corresponding test cases
 * (`ClassTypeCreatorDslTestCases`) is located in [TestCases.testClassesMapping].
 */
abstract class AbstractTypeCreatorDslTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actualText = analyzeForTest(mainFile) {
            val caretToType = testServices.expressionMarkerProvider.getAllCarets(mainFile).associate { caret ->
                val qualifier = caret.qualifier
                val expressionType = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtExpression>(
                    mainFile,
                    qualifier
                ).expressionType ?: error("Unable to retrieve type from `${caret.qualifier}` caret")

                qualifier to expressionType
            }

            val testName = testServices.testInfo.methodName
            val directoryName = testServices.testInfo.className.substringAfterLast("$")

            val result =
                TestCases.runTest(directoryName, testName, this, caretToType)

            render(result)
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }

    private fun KaSession.render(value: Any?): String {
        val renderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
            this.capturedTypeRenderer = KaCapturedTypeRenderer.AS_CAPTURED_TYPE_WITH_PROJECTION
        }
        return buildString {
            when (value) {
                is KaType -> {
                    appendLine("KaType")
                    appendLine(DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value))
                    appendLine()
                    appendLine("Rendered type:")
                    appendLine(value.render(renderer, position = Variance.INVARIANT))
                }
                is KaTypeArgumentWithVariance -> {
                    appendLine("KaTypeArgumentWithVariance")
                    appendLine("Variance: ${value.variance.name}")
                    appendLine("Type: ${DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value.type)}")
                    appendLine()
                    appendLine("Rendered type argument:")
                    appendLine("${value.variance.name} ${value.type.render(renderer, position = Variance.INVARIANT)}")
                }
                is KaContextReceiver -> {
                    appendLine("KaContextReceiver")
                    appendLine("Label: ${value.label}")
                    appendLine("Type: ${DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value.type)}")
                    appendLine()
                    appendLine("Rendered context receiver:")
                    appendLine("${value.label}: ${value.type.render(renderer, position = Variance.INVARIANT)}")
                }
                is KaFunctionValueParameter -> {
                    appendLine("KaFunctionValueParameter")
                    appendLine("Name: ${value.name}")
                    appendLine("Type: ${DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value.type)}")
                    appendLine()
                    appendLine("Rendered function value parameter:")
                    appendLine("${value.name}: ${value.type.render(renderer, position = Variance.INVARIANT)}")
                }
                is KaStarTypeProjection -> {
                    appendLine("KaStarTypeProjection")
                }
                null -> {
                    appendLine("null")
                }
                else -> error("Unable to render ${value::class}")
            }
        }
    }

    sealed class TestCases(protected val session: KaSession, private val caretToType: Map<String, KaType>) {
        companion object {
            fun runTest(directoryName: String, testName: String, session: KaSession, caretToType: Map<String, KaType>): Any? {
                val testClass = testClassesMapping.getOrElse(directoryName) {
                    error("Unable to find test class for $testName")
                }

                val testClassInstance =
                    testClass.primaryConstructor?.call(session, caretToType) ?: error("Cannot construct test class for $directoryName")

                val testCase = testClass.members.filterIsInstance<KFunction<*>>().single { it.name == testName }

                return testCase.call(testClassInstance)
            }

            private val testClassesMapping: Map<String, KClass<out TestCases>> = mapOf(
                "ClassType" to ClassTypeCreatorDslTestCases::class,
            )
        }

        protected fun getTypeByCaret(label: String): KaType {
            return caretToType[label] ?: error("No type for `$label`")
        }

        protected fun getClassLikeSymbolByCaret(label: String): KaClassLikeSymbol {
            return caretToType[label]?.symbol ?: error("No symbol for `$label`")
        }

        protected fun getTypeParameterSymbolByCaret(label: String): KaTypeParameterSymbol {
            return (caretToType[label] as? KaTypeParameterType)?.symbol ?: error("Type under `$label` is not a type parameter type")
        }
    }
}