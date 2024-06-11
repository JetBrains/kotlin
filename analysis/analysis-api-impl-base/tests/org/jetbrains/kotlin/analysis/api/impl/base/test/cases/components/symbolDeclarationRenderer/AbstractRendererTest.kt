/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies.KaRendererBodyMemberScopeSorter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaClassifierBodyRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractRendererTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val renderer = KaDeclarationRendererForSource.WITH_SHORT_NAMES.with {
            classifierBodyRenderer = KaClassifierBodyRenderer.BODY_WITH_MEMBERS
            bodyMemberScopeSorter = object : KaRendererBodyMemberScopeSorter {
                override fun sortMembers(
                    analysisSession: KaSession,
                    members: List<KaDeclarationSymbol>,
                    owner: KaSymbolWithMembers,
                ): List<KaDeclarationSymbol> {
                    with(analysisSession) {
                        return KaRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINING
                            .sortMembers(analysisSession, members, owner)
                            .sortedBy { it.render() }
                    }
                }
            }
        }

        val actual = executeOnPooledThreadInReadAction {
            buildString {
                mainFile.declarations.forEach { declaration ->
                    analyseForTest(declaration) {
                        append(declaration.symbol.render(renderer))
                        appendLine()
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".rendered")
    }
}
