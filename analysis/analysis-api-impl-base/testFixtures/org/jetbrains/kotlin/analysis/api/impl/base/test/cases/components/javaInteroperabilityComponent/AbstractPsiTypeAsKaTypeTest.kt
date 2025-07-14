/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.render
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractPsiTypeAsKaTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val (psiDeclaration, useSitePosition) = getTestDataContext(testServices)

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                // In the case of copy analysis, we analyze the original `psiDeclaration` because it's an element from another file, which
                // won't be copied by `copyAwareAnalyzeForTest`.
                copyAwareAnalyzeForTest(mainFile) { _ ->
                    val psiType = when (psiDeclaration) {
                        is PsiMethod -> psiDeclaration.returnType
                        is PsiVariable -> psiDeclaration.type
                        else -> error("Unsupported declaration type: ${psiDeclaration::class.simpleName}")
                    }

                    testServices.assertions.assertNotNull(psiType)
                    val kaType = psiType!!.asKaType(useSitePosition ?: psiDeclaration)!!
                    appendLine("${PsiType::class.simpleName}: ${psiType.render()}")
                    appendLine("${KaType::class.simpleName}: ${kaType.render(useSiteSession)}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

private data class TestDataContext(val targetDeclaration: PsiElement, val useSitePosition: PsiElement?)

private fun getTestDataContext(testServices: TestServices): TestDataContext {
    var psiDeclaration: PsiElement? = null
    var useSitePosition: PsiElement? = null

    testServices.ktTestModuleStructure.mainModules.forEach { ktTestModule ->
        val psiFiles = ktTestModule.psiFiles
        for (psiFile in psiFiles) {
            val targetOffset = testServices.expressionMarkerProvider.getCaretOrNull(psiFile)
            if (targetOffset != null) {
                if (psiDeclaration != null) error("Only one target method is expected")
                psiDeclaration = psiFile.findElementAt(targetOffset)?.parentsWithSelf?.find { it is PsiMethod || it is PsiVariable }
            }

            val useSiteOffset = testServices.expressionMarkerProvider.getCaretOrNull(psiFile, qualifier = "useSite")
            if (useSiteOffset != null) {
                if (useSitePosition != null) error("Only one target method is expected")
                useSitePosition = psiFile.findElementAt(useSiteOffset)?.parentOfType<PsiElement>(withSelf = true)
            }
        }
    }

    return TestDataContext(psiDeclaration ?: error("Target method is not found"), useSitePosition)
}
