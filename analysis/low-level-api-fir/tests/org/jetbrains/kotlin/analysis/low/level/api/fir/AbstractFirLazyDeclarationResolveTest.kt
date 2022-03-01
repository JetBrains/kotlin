/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * Test that we do not resolve declarations we do not need & do not build bodies for them
 */
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractFirLazyDeclarationResolveTest : AbstractLowLevelApiSingleFileTest() {

    private fun FirFile.findResolveMe(): FirDeclaration {
        val visitor = object : FirVisitorVoid() {
            var result: FirDeclaration? = null
            override fun visitElement(element: FirElement) {
                if (result != null) return
                val declaration = element.realPsi as? KtDeclaration
                if (element is FirDeclaration && declaration != null && declaration.name == "resolveMe") {
                    result = element
                    return
                }
                element.acceptChildren(this)
            }

        }
        accept(visitor)
        return visitor.result ?: error("declaration with name `resolveMe` was not found")
    }

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val rendererOption = FirRenderer.RenderMode.WithDeclarationAttributes.copy(renderDeclarationResolvePhase = true)
        val resultBuilder = StringBuilder()
        resolveWithClearCaches(ktFile) { firModuleResolveState ->
            check(firModuleResolveState is LLFirSourceModuleResolveState)
            val declarationToResolve = firModuleResolveState
                .getOrBuildFirFile(ktFile)
                .findResolveMe()
            for (currentPhase in FirResolvePhase.values()) {
                if (currentPhase == FirResolvePhase.SEALED_CLASS_INHERITORS) continue
                declarationToResolve.ensureResolved(currentPhase)
                val firFile = firModuleResolveState.getOrBuildFirFile(ktFile)
                resultBuilder.append("\n${currentPhase.name}:\n")
                resultBuilder.append(firFile.render(rendererOption))
            }
        }

        resolveWithClearCaches(ktFile) { firModuleResolveState ->
            check(firModuleResolveState is LLFirSourceModuleResolveState)
            val firFile = firModuleResolveState.getOrBuildFirFile(ktFile)
            firFile.ensureResolved(FirResolvePhase.BODY_RESOLVE)
            resultBuilder.append("\nFILE RAW TO BODY:\n")
            resultBuilder.append(firFile.render(rendererOption))
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(resultBuilder.toString())
    }

    override val enableTestInDependedMode: Boolean = false

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}
