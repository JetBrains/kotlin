/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDeprecationsResolveTest : AbstractFirLazyDeclarationResolveTestCase() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val ktDeclaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(mainFile)

        val symbolGetters = when (ktDeclaration) {
            is KtProperty -> listOf(
                { s: KaSymbol -> s } to "property",
                { s: KaSymbol -> (s as KaPropertySymbol).getter } to "getter",
                { s: KaSymbol -> (s as KaPropertySymbol).setter } to "setter",
                { s: KaSymbol -> (s as KaPropertySymbol).backingFieldSymbol } to "backingField",
            )
            is KtNamedFunction -> listOf(
                { s: KaSymbol -> s } to "function",
            )
            is KtClassOrObject -> listOf(
                { s: KaSymbol -> s } to "class",
            )
            is KtTypeAlias -> listOf(
                { s: KaSymbol -> s } to "alias",
            )
            else -> error("Unexpected element: $ktDeclaration")
        }

        for ((symbolGetter, name) in symbolGetters) {
            analyseForTest(ktDeclaration) {
                testSymbol(ktDeclaration.symbol, symbolGetter, testServices, name)
            }
            LLFirSessionInvalidationService.getInstance(mainFile.project).invalidateAll(includeLibraryModules = true)
        }

    }

    private fun KaSession.testSymbol(
        rootSymbol: KaSymbol,
        symbolGetter: (KaSymbol) -> KaSymbol?,
        testServices: TestServices,
        name: String,
    ) {
        val rootSymbolFir = getFirSymbol(rootSymbol)
        val beforeRendered = renderFirElement(rootSymbolFir.fir)
        val targetSymbol = symbolGetter(rootSymbol) ?: return
        testServices.assertions.assertEqualsToTestDataFileSibling(beforeRendered, extension = ".${name}.before.txt")
        val deprecationStatus = targetSymbol.deprecationStatus
        testServices.assertions.assertEqualsToTestDataFileSibling(
            renderFirElement(rootSymbolFir.fir),
            extension = ".${name}.after.txt"
        )
        testServices.assertions.assertEqualsToTestDataFileSibling(deprecationStatus.toString(), extension = ".${name}.out.txt")
    }

    private fun getFirSymbol(kaSymbol: KaSymbol): FirBasedSymbol<*> {
        val kaFirSymbolJavaClass = Class.forName("org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol")
        return kaFirSymbolJavaClass
            .getDeclaredMethod("getFirSymbol").invoke(kaSymbol) as FirBasedSymbol<*>
    }

    private fun renderFirElement(fir: FirDeclaration): String {
        val renderer = FirRenderer(
            builder = StringBuilder(),
            classMemberRenderer = null,
            contractRenderer = null,
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
        )
        return renderer.renderElementAsString(fir)
    }
}

abstract class AbstractSourceDeprecationsResolveTest : AbstractDeprecationsResolveTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}