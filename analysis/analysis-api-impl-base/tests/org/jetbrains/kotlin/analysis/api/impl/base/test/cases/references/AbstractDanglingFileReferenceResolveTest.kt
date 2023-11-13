/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractDanglingFileReferenceResolveTest : AbstractReferenceResolveTest() {
    override fun KtAnalysisSession.getAdditionalSymbolInfo(symbol: KtSymbol): String? {
        val containingFile = symbol.psi?.containingFile ?: return null
        return containingFile.name
    }

    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val mainModule = findMainModule(moduleStructure)
        val ktFiles = testServices.ktModuleProvider.getModuleFiles(mainModule).filterIsInstance<KtFile>()
        val mainKtFile = findMainFile(ktFiles)
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(mainKtFile)

        val ktPsiFactory = KtPsiFactory.contextual(mainKtFile, markGenerated = true, eventSystemEnabled = true)
        val fakeKtFile = ktPsiFactory.createFile("fake.kt", mainKtFile.text)

        doTestByFileStructure(fakeKtFile, caretPosition, mainModule, testServices)
    }
}