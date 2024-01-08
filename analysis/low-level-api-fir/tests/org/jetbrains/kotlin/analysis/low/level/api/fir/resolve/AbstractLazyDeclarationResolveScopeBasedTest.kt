/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithClearCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.scopes.processAllOverriddenCallables
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * This test exists to check lazy resolution for fake overrides
 */
abstract class AbstractLazyDeclarationResolveScopeBasedTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val classOrObject = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtClassOrObject>(mainFile)
        resolveWithClearCaches(classOrObject) { session ->
            val classSymbol = classOrObject.resolveToFirSymbolOfType<FirClassSymbol<*>>(session)
            val symbols = collectAllCallableDeclarations(classSymbol, session)
            val dumpBefore = dumpSymbols(symbols)
            testServices.assertions.assertEqualsToTestDataFileSibling(dumpBefore, extension = "before.txt")
            for (callableSymbol in symbols) {
                callableSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            }

            val dumpAfter = dumpSymbols(symbols)
            testServices.assertions.assertEqualsToTestDataFileSibling(dumpAfter, extension = "after.txt")
        }
    }
}

private fun collectAllCallableDeclarations(classSymbol: FirClassSymbol<*>, session: LLFirResolveSession): Collection<FirCallableSymbol<*>> {
    val baseScope = classSymbol.unsubstitutedScope(
        session.useSiteFirSession,
        session.getScopeSessionFor(session.useSiteFirSession),
        false,
        FirResolvePhase.STATUS,
    )

    return buildSet {
        baseScope.processAllCallables { callable ->
            add(callable)
            baseScope.processAllOverriddenCallables(
                callable,
                processor = {
                    add(it)
                    ProcessorAction.NEXT
                },
                processDirectOverriddenCallablesWithBaseScope = { declaration, processor ->
                    if (declaration is FirPropertySymbol) {
                        processDirectOverriddenPropertiesWithBaseScope(declaration, processor)
                    } else {
                        declaration as FirNamedFunctionSymbol
                        processDirectOverriddenFunctionsWithBaseScope(declaration, processor)
                    }
                }
            )
        }
    }
}

private fun dumpSymbols(symbols: Collection<FirCallableSymbol<*>>): String {
    val builder = StringBuilder()
    val renderer = lazyResolveRenderer(builder)
    for (callableSymbol in symbols) {
        if (builder.isNotEmpty()) builder.appendLine()
        renderer.renderElementAsString(callableSymbol.fir)
    }

    return builder.toString()
}

abstract class AbstractSourceLazyDeclarationResolveScopeBasedTest : AbstractLazyDeclarationResolveScopeBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest : AbstractLazyDeclarationResolveScopeBasedTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptLazyDeclarationResolveScopeBasedTest : AbstractLazyDeclarationResolveScopeBasedTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
