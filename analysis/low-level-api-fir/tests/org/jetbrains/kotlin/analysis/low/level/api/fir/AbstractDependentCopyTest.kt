/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir.getFirResolveSessionForDependentCopy
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * This test class is supposed to specify the behavior of [getFirResolveSessionForDependentCopy].
 * E.g., end test can check how a copied declaration is resolved or which content has.
 *
 * @see AbstractDependentCopyContextTest
 * @see AbstractDependentCopyFirTest
 */
abstract class AbstractDependentCopyTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val moduleStructure = testServices.moduleStructure
        val element = testServices.expressionMarkerProvider.getElementOfTypeAtCaretByDirective<KtElement>(
            file = mainFile,
            registeredDirectives = moduleStructure.allDirectives,
        )

        val specialContentForCopiedFile = getTestDataFileSiblingPath(
            extension = "copy.txt",
            testPrefix = null,
        ).takeIf { it.exists() }?.readText()

        val fileCopy = KtPsiFactory(mainFile.project).createFile(
            mainFile.name,
            specialContentForCopiedFile ?: mainFile.text,
        )

        fileCopy.originalFile = mainFile

        val sameElementInCopy = PsiTreeUtil.findSameElementInCopy(element, fileCopy)
        resolveWithClearCaches(mainFile) { originalSession ->
            val dependentSession = getFirResolveSessionForDependentCopy(
                originalFirResolveSession = originalSession,
                originalKtFile = mainFile,
                elementToAnalyze = sameElementInCopy,
            ) as LLFirResolveSessionDepended

            with(dependentSession) {
                doDependentCopyTest(fileCopy, sameElementInCopy, moduleStructure, testServices)
            }
        }
    }

    context(LLFirResolveSessionDepended)
    abstract fun doDependentCopyTest(file: KtFile, element: KtElement, moduleStructure: TestModuleStructure, testServices: TestServices)
}
