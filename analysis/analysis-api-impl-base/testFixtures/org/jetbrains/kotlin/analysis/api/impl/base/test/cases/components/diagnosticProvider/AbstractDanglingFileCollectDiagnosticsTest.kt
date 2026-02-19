/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractDanglingFileCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_DANGLING_FILES by stringDirective("Ignore dangling file diagnostic tests.")
        val IGNORE_SELF_MODE by stringDirective("Whether to use ${KaDanglingFileResolutionMode.IGNORE_SELF} mode.")
    }

    override fun doTest(testServices: TestServices) {
        testServices.moduleStructure.allDirectives.suppressIf(
            suppressionDirective = Directives.IGNORE_DANGLING_FILES,
            filter = { it is AssertionError },
            action = {
                super.doTest(testServices)
            }
        )
    }

    override fun prepareKtFile(ktFile: KtFile, testServices: TestServices): PreparedFile {
        val contextModule = KotlinProjectStructureProvider.getModule(ktFile.project, ktFile, useSiteModule = null)
        val ktPsiFactory = KtPsiFactory.contextual(ktFile, markGenerated = true, eventSystemEnabled = false)

        val fakeFile = ktPsiFactory.createFile("fake.kt", ktFile.text).apply {
            this.contextModule = contextModule
            if (Directives.IGNORE_SELF_MODE in testServices.moduleStructure.allDirectives) {
                originalFile = ktFile
            }
        }

        return PreparedFile(fakeFile, ktFile.name)
    }
}
