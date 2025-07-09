/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractDanglingFileCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    override fun prepareKtFile(ktFile: KtFile, testServices: TestServices): KtFile {
        val contextModule = KotlinProjectStructureProvider.getModule(ktFile.project, ktFile, useSiteModule = null)
        val ktPsiFactory = KtPsiFactory.contextual(ktFile, markGenerated = true, eventSystemEnabled = false)

        return ktPsiFactory.createFile("fake.kt", ktFile.text).also { ktFile ->
            ktFile.contextModule = contextModule
        }
    }
}
