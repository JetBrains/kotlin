/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiAnnotationsOnDeclarationsTest : AbstractAnalysisApiBasedTest() {
    context(session: KaSession)
    open fun renderAnnotations(annotations: KaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotations(annotations)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val ktDeclaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val actual = copyAwareAnalyzeForTest(ktDeclaration) { contextDeclaration ->
            val declarationSymbol = contextDeclaration.symbol as KaAnnotatedSymbol
            buildString {
                appendLine("${KtDeclaration::class.simpleName}: ${contextDeclaration::class.simpleName} ${contextDeclaration.name}")
                append(renderAnnotations(declarationSymbol.annotations))
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
