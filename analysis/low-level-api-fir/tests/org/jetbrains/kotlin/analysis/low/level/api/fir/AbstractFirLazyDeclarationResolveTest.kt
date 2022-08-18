/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithAttributes
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
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
                if (element is FirDeclaration && declaration != null && declaration.name?.decapitalizeAsciiOnly() == "resolveMe") {
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
        val resultBuilder = StringBuilder()
        val renderer = FirRenderer(
            builder = resultBuilder,
            declarationRenderer = FirDeclarationRendererWithAttributes(),
            resolvePhaseRenderer = FirResolvePhaseRenderer()
        )
        resolveWithClearCaches(ktFile) { firResolveSession ->
            check(firResolveSession is LLFirSourceResolveSession)
            val declarationToResolve = firResolveSession
                .getOrBuildFirFile(ktFile)
                .findResolveMe()
            for (currentPhase in FirResolvePhase.values()) {
                if (currentPhase == FirResolvePhase.SEALED_CLASS_INHERITORS) continue
                declarationToResolve.lazyResolveToPhase(currentPhase)
                val firFile = firResolveSession.getOrBuildFirFile(ktFile)
                resultBuilder.append("\n${currentPhase.name}:\n")
                renderer.renderElementAsString(firFile)
            }
        }

        resolveWithClearCaches(ktFile) { firResolveSession ->
            check(firResolveSession is LLFirSourceResolveSession)
            val firFile = firResolveSession.getOrBuildFirFile(ktFile)
            firFile.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            resultBuilder.append("\nFILE RAW TO BODY:\n")
            renderer.renderElementAsString(firFile)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(resultBuilder.toString())
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}
