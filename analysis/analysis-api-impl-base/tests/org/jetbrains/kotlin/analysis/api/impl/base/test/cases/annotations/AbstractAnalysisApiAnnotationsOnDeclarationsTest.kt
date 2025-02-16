/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiAnnotationsOnDeclarationsTest : AbstractAnalysisApiBasedTest() {
    open fun renderAnnotations(analysisSession: KaSession, annotations: KaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotations(analysisSession, annotations)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val ktDeclaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val actual = analyseForTest(ktDeclaration) {
            val declarationSymbol = ktDeclaration.symbol as KaAnnotatedSymbol
            buildString {
                appendLine("${KtDeclaration::class.simpleName}: ${ktDeclaration::class.simpleName} ${ktDeclaration.name}")
                append(renderAnnotations(useSiteSession, declarationSymbol.annotations))
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}

