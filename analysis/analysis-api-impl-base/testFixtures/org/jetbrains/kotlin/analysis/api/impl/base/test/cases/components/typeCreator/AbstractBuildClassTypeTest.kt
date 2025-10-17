/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator

import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

/**
 * A test for [org.jetbrains.kotlin.analysis.api.components.KaTypeCreator.buildClassType].
 *
 * The test works by retrieving the type of the selected expression (in `<expr>` tag) and building two class types
 * based on it: one using its symbol and one using its classId.
 *
 * The test also uses several directives to control the class type creation:
 * #### [Directives.NULLABLE]
 * Sets [org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder.isMarkedNullable] to `true`.
 *
 * #### [Directives.ARGUMENT]
 * Adds a type argument to the resulting class type.
 * All type arguments are registered in the order of [Directives.ARGUMENT] directives appearance in the test file.
 * The argument can be either a star projection (`*`), or a type argument with a variance (e.g., `in Int`).
 *
 * To add a star projection, `ARGUMENT: STAR` can be used.
 *
 * To add a type argument with a variance, `ARGUMENT: VARIANCE_ID` should be used.
 * `VARIANCE` can be one of `IN`, `OUT`, or `INV`.
 * `ID` corresponds to the caret position in the test file from which the type for the type argument should be retrieved.
 * E.g., `ARGUMENT: INV_2` adds an invariant type argument with a type retrieved from the expression under `<caret_2>` in the file.
 *
 * The ID must be a number. The ID itself doesn't matter, as long as it has a corresponding caret in the file.
 * Several type argument directives can use the same caret expression,
 * e.g., `ARGUMENT: INV_1 OUT_1` adds two type arguments with the same type but different variances.
 */
abstract class AbstractBuildClassTypeTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val targetExpression = testServices.expressionMarkerProvider
                .getBottommostSelectedElementOfType(contextFile, KtExpression::class)
            val expressionType = targetExpression.expressionType?.abbreviationOrSelf ?: error("Expression type is null")
            val allTypesById = testServices.expressionMarkerProvider.getAllCarets(contextFile).associate { caret ->
                val qualifier = caret.qualifier
                val caretExpression =
                    testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtExpression>(contextFile, qualifier)
                val expressionType =
                    caretExpression.expressionType?.abbreviationOrSelf ?: error("Expression under $qualifier doesn't have a type")
                val id = caret.qualifier.toIntOrNull() ?: error("Caret qualifier $qualifier is not a number")

                id to expressionType
            }
            val argumentDirectives = mainModule.testModule.directives[Directives.ARGUMENT]

            val isMarkedNullable = Directives.NULLABLE in mainModule.testModule.directives

            val builderConfiguration: KaClassTypeBuilder.() -> Unit = {
                this.isMarkedNullable = isMarkedNullable

                argumentDirectives.forEach { typeArgument ->
                    when (typeArgument) {
                        is TypeArgument.StarProjection -> argument(buildStarTypeProjection())
                        is TypeArgument.TypeArgumentWithVariance -> {
                            val type = allTypesById[typeArgument.caretId] ?: error("No type with id ${typeArgument.caretId}")
                            argument(type, typeArgument.variance)
                        }
                    }
                }
            }


            val symbol = expressionType.symbol ?: error("Expression type does not have a symbol")
            buildString {
                appendLine("CLASS_TYPE_BY_CLASS_ID")

                val classId = symbol.classId
                if (classId == null) {
                    appendLine("   ClassId is null")
                } else {
                    val classTypeByClassId = buildClassType(classId, builderConfiguration)
                    appendLine("TYPE:")
                    appendLine(
                        DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, classTypeByClassId)
                    )
                    appendLine("RENDERED TYPE:")
                    appendLine(
                        classTypeByClassId.render(
                            renderer = KaTypeRendererForDebug.WITH_QUALIFIED_NAMES,
                            position = Variance.INVARIANT,
                        )
                    )
                }

                val classTypeBySymbol = buildClassType(symbol, builderConfiguration)

                appendLine()
                appendLine("CLASS_TYPE_BY_SYMBOL")
                appendLine("TYPE:")
                appendLine(
                    DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, classTypeBySymbol)
                )
                appendLine("RENDERED TYPE:")
                appendLine(
                    classTypeBySymbol.render(
                        renderer = KaTypeRendererForDebug.WITH_QUALIFIED_NAMES,
                        position = Variance.INVARIANT,
                    )
                )
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private sealed class TypeArgument {
        class StarProjection : TypeArgument()
        class TypeArgumentWithVariance(val variance: Variance, val caretId: Int) : TypeArgument()
    }

    private object Directives : SimpleDirectivesContainer() {
        val NULLABLE by directive("Make the resulting type nullable")
        val ARGUMENT by valueDirective("Type argument to use for class creation") { string ->
            val splits = string.split("_")
            val variance = splits.first()
            val parsedVariance = when (variance.uppercase()) {
                "IN" -> Variance.IN_VARIANCE
                "OUT" -> Variance.OUT_VARIANCE
                "INV" -> Variance.INVARIANT
                "STAR" -> null
                else -> error("Unknown variance: $string")
            }

            if (parsedVariance == null) {
                return@valueDirective TypeArgument.StarProjection()
            }

            val id = splits.getOrNull(1)?.toIntOrNull() ?: error("Cannot parse caret id from $string")

            return@valueDirective TypeArgument.TypeArgumentWithVariance(parsedVariance, id)
        }
    }
}
