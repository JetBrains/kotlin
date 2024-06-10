/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.inheritorsProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSealedInheritorsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        doTestByKtFile(mainFile, testServices)
    }

    /**
     * [ktFile] may be a fake file for dangling module tests.
     */
    protected fun doTestByKtFile(ktFile: KtFile, testServices: TestServices) {
        analyseForTest(ktFile) {
            val classSymbol = getSingleTestTargetSymbolOfType<KaNamedClassOrObjectSymbol>(ktFile, testDataPath)

            val actualText = classSymbol.sealedClassInheritors.joinToString("\n\n") { inheritor ->
                "${inheritor.classId!!}\n${inheritor.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)}"
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
        }
    }
}
