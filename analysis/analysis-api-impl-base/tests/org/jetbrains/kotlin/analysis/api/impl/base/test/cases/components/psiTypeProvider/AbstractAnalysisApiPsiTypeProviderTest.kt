/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiPsiTypeProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val psiContext = if (KtPsiUtil.isLocal(declaration)) {
            declaration
        } else {
            val containingClass = getContainingKtLightClass(declaration, mainFile)
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
