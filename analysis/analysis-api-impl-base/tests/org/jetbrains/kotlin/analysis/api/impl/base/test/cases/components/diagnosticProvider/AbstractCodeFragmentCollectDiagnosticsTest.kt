/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class AbstractCodeFragmentCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val contextElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtElement>(mainFile)

        val fragmentText = mainModule.files.single().originalFile
            .run { File(parent, "$nameWithoutExtension.fragment.$extension") }
            .readText()

        val isBlockFragment = fragmentText.any { it == '\n' }

        val project = mainFile.project
        val factory = KtPsiFactory(project, markGenerated = false)

        val codeFragment = when {
            isBlockFragment -> factory.createBlockCodeFragment(fragmentText, contextElement)
            else -> factory.createExpressionCodeFragment(fragmentText, contextElement)
        }

        super.doTestByMainFile(codeFragment, mainModule, testServices)
    }
}