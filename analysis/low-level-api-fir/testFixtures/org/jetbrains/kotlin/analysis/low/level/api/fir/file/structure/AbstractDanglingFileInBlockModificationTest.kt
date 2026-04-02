/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractDanglingFileInBlockModificationTes : AbstractInBlockModificationTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider.getBottommostSelectedElementOfTypeByDirective(mainFile, mainModule)

        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)
        val fakeKtFile = ktPsiFactory.createFile(mainFile.name, mainFile.text)
        val fakeSelectedElement = PsiTreeUtil.findSameElementInCopy(selectedElement, fakeKtFile)

        doTest(fakeKtFile, fakeSelectedElement, testServices)
    }
}

abstract class AbstractSourceLikeDanglingFileInBlockModificationTest : AbstractDanglingFileInBlockModificationTes() {
    override val configurator = LLSourceLikeTestConfigurator()
}

abstract class AbstractOutOfContentRootDanglingFileInBlockModificationTest : AbstractDanglingFileInBlockModificationTes() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}
