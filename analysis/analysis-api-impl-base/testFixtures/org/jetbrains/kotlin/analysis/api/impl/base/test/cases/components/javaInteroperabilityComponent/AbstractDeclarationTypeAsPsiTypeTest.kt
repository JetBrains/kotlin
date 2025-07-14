/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.render
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDeclarationTypeAsPsiTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val psiContext = if (KtPsiUtil.isLocal(declaration)) {
            declaration
        } else {
            val containingClass = getContainingKtLightClass(declaration, mainFile)
            containingClass.findLightDeclarationContext(declaration) ?: error("Can't find psi context for $declaration")
        }

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
                    val kaType = contextDeclaration.returnType
                    val psiType = kaType.asPsiType(psiContext, allowErrorTypes = false)
                    appendLine("${KaType::class.simpleName}: ${kaType.render(useSiteSession)}")
                    appendLine("${PsiType::class.simpleName}: ${psiType?.render()}")
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
