/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.PrivateForInline

abstract class AbstractAnalysisApiKtTypeByPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    @OptIn(PrivateForInline::class)
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val ktFile = moduleStructure.modules.map { testServices.ktModuleProvider.getModuleFiles(it).filterIsInstance<KtFile>().first() }.first()
        val psiMethod = moduleStructure.modules.map { module ->
            val psiFiles = testServices.ktModuleProvider.getModuleFiles(module).filterIsInstance<PsiJavaFile>()
            val javaFile = psiFiles.first()
            val offset = testServices.expressionMarkerProvider.carets.getCaretOffset(javaFile.name, null)!!
            PsiTreeUtil.getParentOfType(javaFile.findElementAt(offset), PsiMethod::class.java)
        }.single()!!

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyseForTest(ktFile) {
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
