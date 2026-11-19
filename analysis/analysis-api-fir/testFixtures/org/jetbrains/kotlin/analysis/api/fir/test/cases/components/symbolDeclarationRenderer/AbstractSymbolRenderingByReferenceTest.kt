/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.KaRendererBodyMemberScopeSorter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES_DENOTABLE
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaClassifierBodyRenderer
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.renderToString
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.analysis.test.framework.utils.resolveSymbolPreferringCall
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolRenderingByReferenceTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtReferenceExpression>(mainFile)

        val renderedString = executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(referenceExpression) { contextReferenceExpression ->
                val ktSymbol = contextReferenceExpression.resolveSymbolPreferringCall()
                testServices.assertions.assertNotNull(ktSymbol)
                testServices.assertions.assertTrue(ktSymbol is KaDeclarationSymbol)
                (ktSymbol as KaDeclarationSymbol).render(renderer)
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(renderedString)
    }

    /**
     * Renders the members of a referenced class as well, so that the test can show the members of library and Java classes, which
     * the renderer tests by PSI cannot render.
     */
    private val renderer = WITH_QUALIFIED_NAMES_DENOTABLE.with {
        classifierBodyRenderer = KaClassifierBodyRenderer.BODY_WITH_MEMBERS

        // Sort the members by their rendered text, so that the order doesn't depend on how a library class is loaded: from stubs in
        // the IDE mode or from class files in the Standalone mode. Companion members go after the other members, so that wrapping
        // them in a `companion { }` block doesn't move the other members.
        bodyMemberScopeSorter = object : KaRendererBodyMemberScopeSorter {
            override fun sortMembers(
                analysisSession: KaSession,
                members: List<KaDeclarationSymbol>,
                container: KaDeclarationContainerSymbol,
            ): List<KaDeclarationSymbol> {
                context(analysisSession) {
                    val sortedMembers =
                        members.sortedWith(compareBy({ it.isCompanionBlockMember }, { KaRenderer.default.renderToString(it) }))
                    return KaRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINING.sortMembers(analysisSession, sortedMembers, container)
                }
            }
        }
    }
}

/** Whether the member is [companion][KaCallableSymbol.isCompanion] and not an enum entry, which stays at the start of an enum body. */
internal val KaSymbol.isCompanionBlockMember: Boolean
    get() = this is KaCallableSymbol && this !is KaEnumEntrySymbol && isCompanion
