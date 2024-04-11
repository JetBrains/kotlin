/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiKtTypeByPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val (psiDeclaration, useSitePosition) = getTestDataContext(testServices)

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyseForTest(mainFile) {
                    val psiType = when (psiDeclaration) {
                        is PsiMethod -> psiDeclaration.returnType
                        is PsiVariable -> psiDeclaration.type
                        else -> error("Unsupported declaration type: ${psiDeclaration::class.simpleName}")
                    }

                    testServices.assertions.assertNotNull(psiType)
                    val ktType = psiType!!.asKtType(useSitePosition ?: psiDeclaration)!!
                    appendLine("PsiType: ${AnalysisApiPsiTypeProviderTestUtils.render(psiType)}")
                    appendLine("KtType: ${AnalysisApiPsiTypeProviderTestUtils.render(analysisSession, ktType)}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}

private data class TestDataContext(val targetDeclaration: PsiElement, val useSitePosition: PsiElement?)

private fun getTestDataContext(testServices: TestServices): TestDataContext {
    var psiDeclaration: PsiElement? = null
    var useSitePosition: PsiElement? = null

    testServices.ktTestModuleStructure.mainModules.forEach { ktTestModule ->
        val psiFiles = ktTestModule.files
        for (psiFile in psiFiles) {
            val targetOffset = testServices.expressionMarkerProvider.getCaretPositionOrNull(psiFile)
            if (targetOffset != null) {
                if (psiDeclaration != null) error("Only one target method is expected")
                psiDeclaration = psiFile.findElementAt(targetOffset)?.parentsWithSelf?.find { it is PsiMethod || it is PsiVariable }
            }

            val useSiteOffset = testServices.expressionMarkerProvider.getCaretPositionOrNull(psiFile, caretTag = "useSite")
            if (useSiteOffset != null) {
                if (useSitePosition != null) error("Only one target method is expected")
                useSitePosition = psiFile.findElementAt(useSiteOffset)?.parentOfType<PsiElement>(withSelf = true)
            }
        }
    }

    return TestDataContext(psiDeclaration ?: error("Target method is not found"), useSitePosition)
}
