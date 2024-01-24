/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractDanglingFileReferenceResolveTest.Directives.COPY_RESOLUTION_MODE
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractDanglingFileReferenceResolveTest : AbstractReferenceResolveTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)

        builder.apply {
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/danglingFileReferenceResolve/ignoreSelf/*") {
                defaultDirectives {
                    COPY_RESOLUTION_MODE.with(DanglingFileResolutionMode.IGNORE_SELF)
                }
            }

            forTestsMatching("analysis/analysis-api/testData/danglingFileReferenceResolve/preferSelf/*") {
                defaultDirectives {
                    COPY_RESOLUTION_MODE.with(DanglingFileResolutionMode.PREFER_SELF)
                }
            }
        }

    }

    override fun KtAnalysisSession.getAdditionalSymbolInfo(symbol: KtSymbol): String? {
        val containingFile = symbol.psi?.containingFile ?: return null
        return containingFile.name
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(mainFile)

        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)
        val fakeKtFile = ktPsiFactory.createFile("fake.kt", mainFile.text)

        if (mainModule.directives.contains(COPY_RESOLUTION_MODE)) {
            fakeKtFile.originalFile = mainFile
        }

        doTestByFileStructure(fakeKtFile, caretPositions, mainModule, testServices)
    }

    override fun <R> analyzeReferenceElement(element: KtElement, mainModule: TestModule, action: KtAnalysisSession.() -> R): R {
        val resolutionMode = mainModule.directives.singleOrZeroValue(COPY_RESOLUTION_MODE)
        return if (resolutionMode != null) {
            analyzeCopy(element, resolutionMode) { action() }
        } else {
            analyze(element) { action() }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val COPY_RESOLUTION_MODE by enumDirective(description = "Dangling file resolution mode for a copy") {
            DanglingFileResolutionMode.valueOf(it)
        }
    }
}