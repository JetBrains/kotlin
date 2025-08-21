/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.suppressIf
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * The handler is responsible for phases verification.
 * It is expected to have all elements in the last resolve phase after diagnostics collector
 * as it has to fully resolve a file.
 */
internal class LLFirPhaseVerifier(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_PHASE_VERIFICATION by stringDirective("Disable phase verifier")
    }

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val visitor = Visitor()
        for (firFile in info.mainFirFiles.values) {
            firFile.accept(visitor)
        }

        module.directives.suppressIf(Directives.IGNORE_PHASE_VERIFICATION, filter = { true }) {
            testServices.assertions.assertTrue(visitor.unresolvedElements.isEmpty()) {
                buildString {
                    appendLine("Unresolved elements found in the FIR file:")
                    for (unresolvedElement in visitor.unresolvedElements) {
                        appendLine("--------------")
                        unresolvedElement.accept(lazyResolveRenderer(this).visitor)
                        appendLine()
                    }
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        testServices.ktTestModuleStructure.allMainKtFiles
    }

    private class Visitor : FirVisitorVoid() {
        val unresolvedElements = mutableListOf<FirElementWithResolveState>()

        override fun visitElement(element: FirElement) {
            if (element is FirElementWithResolveState && element.resolvePhase < FirResolvePhase.entries.last()) {
                unresolvedElements += element
            }

            element.acceptChildren(this)
        }
    }
}