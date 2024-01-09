/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractDanglingFileCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = false)
        val fakeKtFile = ktPsiFactory.createFile("fake.kt", mainFile.text)

        super.doTestByMainFile(fakeKtFile, mainModule, testServices)
    }
}
