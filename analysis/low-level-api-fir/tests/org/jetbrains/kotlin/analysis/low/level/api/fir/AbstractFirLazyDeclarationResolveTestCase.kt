/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder.findElementIn
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithFilteredAttributes
import org.jetbrains.kotlin.fir.renderer.FirErrorExpressionExtendedRenderer
import org.jetbrains.kotlin.fir.renderer.FirFileAnnotationsContainerRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Test that we do not resolve declarations we do not need & do not build bodies for them
 */
abstract class AbstractFirLazyDeclarationResolveTestCase : AbstractAnalysisApiBasedTest() {
    protected fun findFirDeclarationToResolve(
        ktFile: KtFile,
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        firResolveSession: LLFirResolveSession,
    ): Pair<FirElementWithResolveState, ((FirResolvePhase) -> Unit)> = when {
        Directives.RESOLVE_FILE_ANNOTATIONS in moduleStructure.allDirectives -> {
            val annotationContainer = firResolveSession.getOrBuildFirFile(ktFile).annotationsContainer!!
            annotationContainer to fun(phase: FirResolvePhase) {
                annotationContainer.lazyResolveToPhase(phase)
            }
        }
        Directives.RESOLVE_FILE in moduleStructure.allDirectives -> {
            val session = firResolveSession.useSiteFirSession as LLFirResolvableModuleSession
            val file = session.moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            file to fun(phase: FirResolvePhase) {
                file.lazyResolveToPhase(phase)
            }
        }
        else -> {
            val ktDeclaration = if (Directives.RESOLVE_SCRIPT in moduleStructure.allDirectives) {
                ktFile.script!!
            } else {
                testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
            }

            val declarationSymbol = ktDeclaration.resolveToFirSymbol(firResolveSession)
            val firDeclaration = chooseMemberDeclarationIfNeeded(declarationSymbol, moduleStructure, firResolveSession)
            firDeclaration.fir to fun(phase: FirResolvePhase) {
                firDeclaration.lazyResolveToPhase(phase)
            }
        }
    }

    protected fun chooseMemberDeclarationIfNeeded(
        symbol: FirBasedSymbol<*>,
        moduleStructure: TestModuleStructure,
        session: LLFirResolveSession,
    ): FirBasedSymbol<*> {
        val directives = moduleStructure.allDirectives
        val memberClassFilters = listOfNotNull(
            directives.singleOrZeroValue(Directives.MEMBER_CLASS_FILTER),
            directives.singleOrZeroValue(Directives.MEMBER_NAME_FILTER),
        ).ifEmpty { return symbol }

        val (classSymbol, declarations) = when (symbol) {
            is FirClassSymbol -> symbol to symbol.declarationSymbols
            is FirScriptSymbol -> {
                symbol to symbol.fir.let { it.parameters + it.declarations }.map { it.symbol }
            }

            else -> error("Unknown container: ${symbol::class.simpleName}")
        }

        val filter = { declaration: FirBasedSymbol<*> -> memberClassFilters.all { it.invoke(declaration) } }
        val filteredSymbols = declarations.filter(filter)
        return when (filteredSymbols.size) {
            0 -> {
                (classSymbol as? FirClassSymbol)?.let { deepSearch(it, session, filter) }
                    ?: error("Empty result for:${declarations.joinToString("\n")}")
            }
            1 -> filteredSymbols.single()
            else -> error("Result ambiguity:\n${filteredSymbols.joinToString("\n")}")
        }.let { resultSymbol ->
            val isGetter = directives.singleOrZeroValue(Directives.IS_GETTER)
            if (isGetter == null) {
                resultSymbol
            } else {
                requireIsInstance<FirPropertySymbol>(resultSymbol)
                if (isGetter) resultSymbol.getterSymbol!! else resultSymbol.setterSymbol!!
            }
        }
    }

    private fun deepSearch(
        classSymbol: FirClassSymbol<*>,
        session: LLFirResolveSession,
        filter: (FirBasedSymbol<*>) -> Boolean,
    ): FirBasedSymbol<*>? {
        val baseScope = classSymbol.unsubstitutedScope(
            session.useSiteFirSession,
            session.getScopeSessionFor(session.useSiteFirSession),
            false,
            FirResolvePhase.STATUS,
        )

        val scopes: List<FirContainingNamesAwareScope> = listOfNotNull(
            baseScope,
            FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
                session.useSiteFirSession,
                classSymbol.defaultType(),
                baseScope,
            )
        )

        val declarations = mutableListOf<FirBasedSymbol<*>>()
        for (typeScope in scopes) {
            val names = typeScope.getCallableNames()
            for (name in names) {
                typeScope.processFunctionsByName(name) {
                    if (filter(it)) {
                        declarations += it
                    }
                }

                typeScope.processPropertiesByName(name) {
                    if (filter(it)) {
                        declarations += it
                    }
                }

                typeScope.processDeclaredConstructors {
                    if (filter(it)) {
                        declarations += it
                    }
                }
            }
        }

        return declarations.singleOrNull() ?: error("Can't choose from:\n${declarations.joinToString("\n")}")
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val MEMBER_CLASS_FILTER: ValueDirective<(FirBasedSymbol<*>) -> Boolean> by valueDirective("Choose member declaration by a declaration class") { value ->
            val clazz = Class.forName(value)
            ({ symbol: FirBasedSymbol<*> ->
                clazz.isInstance(symbol)
            })
        }

        val MEMBER_NAME_FILTER: ValueDirective<(FirBasedSymbol<*>) -> Boolean> by valueDirective("Choose member declaration by a declaration name") { value ->
            { symbol: FirBasedSymbol<*> ->
                symbol.name() == value
            }
        }

        val IS_GETTER by valueDirective("Choose getter/setter in the case of property", parser = String::toBooleanStrict)

        val RESOLVE_FILE_ANNOTATIONS by directive("Resolve file annotations instead of declaration at caret")
        val RESOLVE_SCRIPT by directive("Resolve script instead of declaration at caret")
        val RESOLVE_FILE by directive("Resolve file instead of declaration at caret")
    }
}

internal fun lazyResolveRenderer(builder: StringBuilder): FirRenderer = FirRenderer(
    builder = builder,
    declarationRenderer = FirDeclarationRendererWithFilteredAttributes(),
    resolvePhaseRenderer = FirResolvePhaseRenderer(),
    errorExpressionRenderer = FirErrorExpressionExtendedRenderer(),
    fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
)

internal operator fun List<FirFile>.contains(element: FirElementWithResolveState): Boolean = if (element is FirFile) {
    element in this
} else {
    any { file ->
        findElementIn<FirElementWithResolveState>(file) {
            it == element
        } != null
    }
}
