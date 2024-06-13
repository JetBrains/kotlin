/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResolveDanglingFileReferenceTest : AbstractResolveReferenceTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)

        builder.apply {
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/danglingFileReferenceResolve/ignoreSelf/*") {
                defaultDirectives {
                    Directives.COPY_RESOLUTION_MODE.with(DanglingFileResolutionMode.IGNORE_SELF)
                }
            }

            forTestsMatching("analysis/analysis-api/testData/danglingFileReferenceResolve/preferSelf/*") {
                defaultDirectives {
                    Directives.COPY_RESOLUTION_MODE.with(DanglingFileResolutionMode.PREFER_SELF)
                }
            }
        }

    }

    override fun KaSession.getAdditionalSymbolInfo(symbol: KaSymbol): String? {
        val containingFile = symbol.psi?.containingFile ?: return null
        return containingFile.name
    }

    override fun collectElementsToResolve(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices
    ): Collection<ResolveTestCaseContext<KtReference?>> {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(mainFile)

        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)
        val fakeKtFile = ktPsiFactory.createFile("fake.kt", mainFile.text)

        if (mainModule.testModule.directives.contains(Directives.COPY_RESOLUTION_MODE)) {
            fakeKtFile.originalFile = mainFile
        }

        return collectElementsToResolve(caretPositions, fakeKtFile)
    }

    override fun <R> analyzeReferenceElement(element: KtElement, mainModule: KtTestModule, action: KaSession.() -> R): R {
        val resolutionMode = mainModule.testModule.directives.singleOrZeroValue(Directives.COPY_RESOLUTION_MODE)
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