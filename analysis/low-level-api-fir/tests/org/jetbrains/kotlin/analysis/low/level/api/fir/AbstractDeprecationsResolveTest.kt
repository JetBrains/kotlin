/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * For a declaration at the caret (which can be a Kotlin or Java declaration), checks how deprecation resolution works
 * and how the FIR tree representation is changed during the resolution.
 *
 * If the declaration at the caret is a `KtProperty`, it checks for the property, getter, setter, and backing field separately.
 */
abstract class AbstractDeprecationsResolveTest : AbstractFirLazyDeclarationResolveTestCase() {
    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val file = mainFile ?: mainModule.psiFiles.first()
        val declaration = when (file) {
            is KtFile -> testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(file)
            is PsiJavaFile -> testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<PsiMember>(file)
            else -> error("Unexpected file ${file::class}")
        }

        val symbolSuppliers = when (declaration) {
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
            is PsiClass -> listOf(
                { s: KaSymbol -> s } to "class",
            )
            is PsiMethod -> listOf(
                { s: KaSymbol -> s } to "method",
            )
            is PsiField -> listOf(
                { s: KaSymbol -> s } to "field",
            )
            else -> error("Unexpected element: $declaration")
        }

        for ((symbolGetter, name) in symbolSuppliers) {
            analyze(mainModule.ktModule) {
                val symbol = when (declaration) {
                    is KtDeclaration -> declaration.symbol
                    is PsiClass -> declaration.namedClassSymbol
                        ?: error("Cannot get symbol for ${declaration::class}")
                    is PsiMember -> declaration.callableSymbol
                        ?: error("Cannot get symbol for ${declaration::class}")
                    else -> error("Unexpected element: $declaration")
                }
                testSymbol(symbol, symbolGetter, testServices, name)
            }
            LLFirSessionInvalidationService.getInstance(file.project).invalidateAll(includeLibraryModules = true)
        }
    }

    private fun KaSession.testSymbol(
        rootSymbol: KaSymbol,
        symbolSupplier: (KaSymbol) -> KaSymbol?,
        testServices: TestServices,
        name: String,
    ) {
        val rootSymbolFir = getFirSymbol(rootSymbol)
        val beforeRendered = renderFirElement(rootSymbolFir.fir)
        val targetSymbol = symbolSupplier(rootSymbol) ?: return
        testServices.assertions.assertEqualsToTestDataFileSibling(beforeRendered, extension = ".${name}.before.txt")
        val deprecationStatus = buildString {
            appendLine("Declaration deprecation: " + targetSymbol.deprecationStatus)
        }
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