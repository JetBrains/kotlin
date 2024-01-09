/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiKtTypeByPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val psiMethod = testServices.moduleStructure.modules.map { module ->
            val psiFiles = testServices.ktModuleProvider.getModuleFiles(module).filterIsInstance<PsiJavaFile>()
            val javaFile = psiFiles.first()
            val offset = testServices.expressionMarkerProvider.getCaretPosition(javaFile)
            PsiTreeUtil.getParentOfType(javaFile.findElementAt(offset), PsiMethod::class.java)
        }.single()!!

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyseForTest(mainFile) {
                    val returnType = psiMethod.returnType
                    testServices.assertions.assertNotNull(returnType)
                    val asKtTypeSuper = returnType!!.asKtType(psiMethod)!!
                    appendLine("PsiType: $returnType")
                    appendLine("KtType: ${asKtTypeSuper.render(position = Variance.OUT_VARIANCE)}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
