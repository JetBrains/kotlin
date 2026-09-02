/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.expressions.expressionType
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.restore
import org.jetbrains.kotlin.analysis.api.types.type
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.types.Variance

abstract class AbstractTypePointerConsistencyTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val targetExpression = testServices.expressionMarkerProvider.getTopmostSelectedElementOfTypeByDirective(mainFile, mainModule)
        val restoreAt = testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtElement>(
            testServices,
            qualifier = "restoreAt"
        ).takeIf { it.isNotEmpty() }?.single()?.first
            ?: mainFile

        val renderer = KaDebugRenderer(renderTypeByProperties = true)

        lateinit var beforeString: String
        lateinit var beforeStringPretty: String
        lateinit var afterString: String
        lateinit var afterStringPretty: String
        lateinit var typePointer: KaTypePointer<*>

        analyzeForTest(mainFile) {
            val type = when (targetExpression) {
                is KtTypeReference -> targetExpression.type
                is KtCallableDeclaration -> (targetExpression.symbol as KaCallableSymbol).returnType
                is KtExpression -> targetExpression.expressionType ?: error("$targetExpression does not have a type")
                else -> error("Unsupported expression type: $targetExpression")
            }

            // Rendering resolves annotation arguments, so the pointer is created first to check that it does not rely on that
            typePointer = type.createPointer()
            beforeString = renderer.renderType(useSiteSession, type)
            beforeStringPretty = type.render(position = Variance.INVARIANT)
        }

        analyzeForTest(restoreAt) {
            val restoredType = typePointer.restore()
            if (restoredType != null) {
                afterString = renderer.renderType(useSiteSession, restoredType)
                afterStringPretty = restoredType.render(position = Variance.INVARIANT)
            } else {
                afterString = "Type pointer restoration failed"
                afterStringPretty = afterString
            }
        }

        val actualText = buildOutputString(beforeString, afterString)
        val actualTextPretty = buildOutputString(beforeStringPretty, afterStringPretty)

        assertEqualsToTestOutputFile(actualText)
        assertEqualsToTestOutputFile(actualTextPretty, extension = ".pretty.txt")
    }

    private fun buildOutputString(beforeString: String, afterString: String): String {
        return if (beforeString == afterString) {
            buildString {
                appendLine("Restored type is the same as the original one").appendLine()
                append(beforeString)
            }
        } else {
            buildString {
                appendLine("Type has changed on restoration").appendLine()
                appendLine("Before:").appendLine()
                appendLine(beforeString).appendLine()
                appendLine("After:").appendLine()
                append(afterString)
            }
        }
    }
}
