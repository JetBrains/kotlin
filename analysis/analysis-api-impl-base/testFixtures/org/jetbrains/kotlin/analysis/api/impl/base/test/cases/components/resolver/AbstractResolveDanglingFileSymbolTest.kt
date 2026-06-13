/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.analyzeCopy
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractResolveDanglingFileSymbolTest : AbstractResolveSymbolTest() {
    protected abstract val shouldCreatePhysicalCopy: Boolean

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)

        builder.apply {
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/components/resolver/danglingFile/ignoreSelf/*") {
                defaultDirectives {
                    Directives.COPY_RESOLUTION_MODE.with(KaDanglingFileResolutionMode.IGNORE_SELF)
                }
            }

            forTestsMatching("analysis/analysis-api/testData/components/resolver/danglingFile/preferSelf/*") {
                defaultDirectives {
                    Directives.COPY_RESOLUTION_MODE.with(KaDanglingFileResolutionMode.PREFER_SELF)
                }
            }
        }

    }

    context(session: KaSession)
    override fun additionalSymbolInfo(attempt: KaSymbolResolutionAttempt): String? {
        return attempt.symbols
            .mapNotNull { it.psi?.containingFile?.name }
            .takeUnless(List<String>::isEmpty)
            ?.toString()
    }

    override fun collectElementsToResolve(
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices
    ): Collection<ResolveTestCaseContext<KtElement>> {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(file)

        val ktPsiFactory = KtPsiFactory.contextual(file, markGenerated = true, eventSystemEnabled = shouldCreatePhysicalCopy)
        val fakeKtFile = ktPsiFactory.createFile("fake.kt", file.text)

        if (module.testModule.directives.contains(Directives.COPY_RESOLUTION_MODE)) {
            if (module.testModule.directives.contains(Directives.USER_DATA_COPY)) {
                fakeKtFile.putUserData(PsiFileFactory.ORIGINAL_FILE, file)
            } else {
                fakeKtFile.originalFile = file
            }
        }

        return caretPositions.map {
            val element = fakeKtFile.findElementAt(it.value)?.getParentOfType<KtElement>(strict = false)!!
            ResolveKtElementTestCaseContext(element = element, marker = it.tagText)
        }
    }

    override fun <R> analyzeSymbolElement(element: KtElement, testServices: TestServices, action: KaSession.() -> R): R {
        val resolutionMode = testServices.moduleStructure.allDirectives.singleOrZeroValue(Directives.COPY_RESOLUTION_MODE)
        return if (resolutionMode != null) {
            analyzeCopy(element, resolutionMode) { action() }
        } else {
            analyze(element) { action() }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val COPY_RESOLUTION_MODE by enumDirective(description = "Dangling file resolution mode for a copy") {
            KaDanglingFileResolutionMode.valueOf(it)
        }

        val USER_DATA_COPY by directive(description = "Provide the original file as a user data property")
    }
}

abstract class AbstractPhysicalResolveDanglingFileSymbolTest : AbstractResolveDanglingFileSymbolTest() {
    override val shouldCreatePhysicalCopy: Boolean
        get() = true
}

abstract class AbstractNonPhysicalResolveDanglingFileSymbolTest : AbstractResolveDanglingFileSymbolTest() {
    override val shouldCreatePhysicalCopy: Boolean
        get() = false
}
