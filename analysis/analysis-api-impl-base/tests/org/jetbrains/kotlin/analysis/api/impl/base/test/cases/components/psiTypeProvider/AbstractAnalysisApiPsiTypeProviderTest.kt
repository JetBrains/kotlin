/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.psiTypeProvider.AnalysisApiPsiTypeProviderTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiPsiTypeProviderTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
        val containingClass = getContainingKtLightClass(declaration, ktFile)
        val psiContext = containingClass.findLightDeclarationContext(declaration)
            ?: error("Can't find psi context for $declaration")
        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyze(declaration) {
                    val ktType = declaration.getReturnKtType()
                    appendLine("KtType: ${ktType.render()}")
                    appendLine("PsiType: ${ktType.asPsiType(psiContext)}")
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
