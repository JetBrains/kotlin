/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.visibilityChecker

import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.visibility.isVisibleInClass
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsVisibleInClassTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val ktClass = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtClassOrObject>(contextFile)
            val classSymbol = ktClass.symbol as KaClassSymbol
            val callableSymbol = getSingleTestTargetSymbolOfType<KaCallableSymbol>(testDataPath, contextFile)

            val isVisible = callableSymbol.isVisibleInClass(classSymbol)
            """
                Declaration: ${callableSymbol.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)}
                Class: ${classSymbol.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)}
                Is visible: $isVisible
            """.trimIndent()
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
