/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.KtRendererBodyMemberScopeSorter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KtClassifierBodyRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractRendererTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val renderer = KtDeclarationRendererForSource.WITH_SHORT_NAMES.with {
            classifierBodyRenderer = KtClassifierBodyRenderer.BODY_WITH_MEMBERS
            bodyMemberScopeSorter = object : KtRendererBodyMemberScopeSorter {
                context(KtAnalysisSession)
                override fun sortMembers(members: List<KtDeclarationSymbol>, owner: KtSymbolWithMembers): List<KtDeclarationSymbol> {
                    return KtRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINING
                        .sortMembers(members, owner)
                        .sortedBy { it.render() }
                }
            }
        }

        val actual = executeOnPooledThreadInReadAction {
            buildString {
                mainFile.declarations.forEach { declaration ->
                    analyseForTest(declaration) {
                        append(declaration.getSymbol().render(renderer))
                        appendLine()
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".rendered")
    }
}
