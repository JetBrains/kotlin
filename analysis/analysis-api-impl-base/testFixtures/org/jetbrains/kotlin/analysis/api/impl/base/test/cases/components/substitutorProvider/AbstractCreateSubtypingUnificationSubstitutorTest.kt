/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substitutorProvider

import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.components.KaUnificationSubstitutorPolicy
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KtAssert.fail
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

/**
 * A test runner for [KaSubstitutorProvider.createSubtypingUnificationSubstitutor].
 *
 * Type pairs are created from expression types in the test data.
 * Such expressions should be marked with <caret_ID_TYPE>, where ID is the index of the pair and TYPE is either `base` for marking
 * the candidate type or `target` for marking the target type. E.g., the first pair is marked with `<caret_1_base>` and `<caret_1_target>`.
 * ID can be anything separated by underscores (`_`).
 *
 * Then [AbstractCreateSubtypingUnificationSubstitutorTest] calls [KaSubstitutorProvider.createSubtypingUnificationSubstitutor] twice with the provided pairs:
 * with [KaUnificationSubstitutorPolicy.UNIVERSAL] and [KaUnificationSubstitutorPolicy.EXISTENTIAL].
 * The rendered result contains the calculated substitutors as well as substituted type pairs.
 */
abstract class AbstractCreateSubtypingUnificationSubstitutorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val candidateToTargetPairs = buildList {
                for (caretIndex in 1..<Int.MAX_VALUE) {
                    fun getExpressionType(caretKind: String): KaType? {
                        val caretQualifier = "${caretIndex}_${caretKind}"
                        val expression = testServices.expressionMarkerProvider
                            .getBottommostElementOfTypeAtCaretOrNull<KtExpression>(contextFile, caretQualifier)
                            ?: return null

                        return expression.expressionType ?: error("No type for $caretQualifier")
                    }

                    val candidateExpressionType = getExpressionType("base")
                    val targetExpressionType = getExpressionType("target")
                    if ((candidateExpressionType == null) xor (targetExpressionType == null)) {
                        // Single type retrieved for the current pair index
                        val failureText = buildString {
                            append("Both 'base' and 'target' markers expected for $caretIndex, got ")
                            append("base = ").append(candidateExpressionType?.render(position = Variance.INVARIANT) ?: "null")
                            append(", target = ").append(targetExpressionType?.render(position = Variance.INVARIANT) ?: "null")
                        }
                        fail(failureText)
                    } else if (candidateExpressionType == null) {
                        // No types found for the current index, assuming there are no more pairs
                        break
                    }

                    add(candidateExpressionType to targetExpressionType!!)
                }
            }

            val substitutorUniversal = createSubtypingUnificationSubstitutor(
                candidateToTargetPairs,
                KaUnificationSubstitutorPolicy.UNIVERSAL
            )
            val substitutorExistential = createSubtypingUnificationSubstitutor(
                candidateToTargetPairs,
                KaUnificationSubstitutorPolicy.EXISTENTIAL
            )

            val result = prettyPrint {
                fun renderActual(substitutor: KaSubstitutor?) {
                    appendLine("Substitutor: ${stringRepresentation(substitutor)}")
                    if (substitutor != null) {
                        appendLine("Substituted pairs:")
                        candidateToTargetPairs.forEach { [candidateType, targetType] ->
                            val substitutedCandidateType = substitutor.substitute(candidateType)
                            val substitutedTargetType = substitutor.substitute(targetType)
                            appendLine("$substitutedCandidateType <: $substitutedTargetType (Original: $candidateType <: $targetType)")
                        }
                    }
                }

                appendLine("UNIVERSAL:")
                renderActual(substitutorUniversal)
                appendLine()
                appendLine("EXISTENTIAL:")
                renderActual(substitutorExistential)
            }

            testServices.assertions.assertEqualsToTestOutputFile(result)
        }
    }
}
