/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val (declaration, ktFile) = moduleStructure.modules.flatMap { module ->
            val ktFiles = testServices.ktModuleProvider.getModuleFiles(module).filterIsInstance<KtFile>()
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtDeclaration>(ktFiles)
        }.single()

        val psiContext = if (KtPsiUtil.isLocal(declaration)) {
            declaration
        } else {
            val containingClass = getContainingKtLightClass(declaration, ktFile)
            containingClass.findLightDeclarationContext(declaration) ?: error("Can't find psi context for $declaration")
        }

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyze(declaration) {
                    val ktType = declaration.getReturnKtType()
                    appendLine("KtType: ${ktType.render(position = Variance.INVARIANT)}")
                    appendLine("PsiType: ${ktType.asPsiType(psiContext, allowErrorTypes = false)}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
