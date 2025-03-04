/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithFilteredAttributes
import org.jetbrains.kotlin.fir.renderer.FirErrorExpressionExtendedRenderer
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
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.*
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Test that we do not resolve declarations we do not need & do not build bodies for them
 */
@OptIn(DirectDeclarationsAccess::class)
abstract class AbstractFirLazyDeclarationResolveTestCase : AbstractAnalysisApiBasedTest() {
    protected fun findFirDeclarationToResolve(
        ktFile: KtFile,
        testServices: TestServices,
        firResolveSession: LLFirResolveSession,
    ): Pair<FirElementWithResolveState, ((FirResolvePhase) -> Unit)> = when {
        Directives.RESOLVE_FILE in testServices.moduleStructure.allDirectives -> {
            val session = firResolveSession.useSiteFirSession as LLFirResolvableModuleSession
            val file = session.moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            file to fun(phase: FirResolvePhase) {
                file.lazyResolveToPhaseByDirective(phase, testServices)
            }
        }
        Directives.RESOLVE_DANGLING_MODIFIER in testServices.moduleStructure.allDirectives -> {
            val session = firResolveSession.useSiteFirSession as LLFirResolvableModuleSession
            val file = session.moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
            val danglingModifier = file.declarations.last() as FirDanglingModifierList
            danglingModifier to fun(phase: FirResolvePhase) {
                danglingModifier.lazyResolveToPhaseByDirective(phase, testServices)
            }
        }
        else -> {
            val ktDeclaration = if (Directives.RESOLVE_SCRIPT in testServices.moduleStructure.allDirectives) {
                ktFile.script!!
            } else {
                testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(ktFile)
            }

            val declarationSymbol = ktDeclaration.resolveToFirSymbol(firResolveSession)
            val firDeclaration = chooseMemberDeclarationIfNeeded(declarationSymbol, testServices.moduleStructure, firResolveSession).fir
            firDeclaration to fun(phase: FirResolvePhase) {
                firDeclaration.lazyResolveToPhaseByDirective(phase, testServices)
            }
        }
    }

    protected fun chooseMemberDeclarationIfNeeded(
        symbol: FirBasedSymbol<*>,
        moduleStructure: TestModuleStructure,
        session: LLFirResolveSession,
    ): FirBasedSymbol<*> {
        val directives = moduleStructure.allDirectives
        val memberSymbol = chooseMemberDeclarationIfNeeded(symbol, session, directives)
        val propertyPart = directives.singleOrZeroValue(Directives.RESOLVE_PROPERTY_PART) ?: return memberSymbol
        requireIsInstance<FirPropertySymbol>(memberSymbol)

        return when (propertyPart) {
            PropertyPart.GETTER -> memberSymbol.getterSymbol!!
            PropertyPart.SETTER -> memberSymbol.setterSymbol!!
            PropertyPart.BACKING_FIELD -> memberSymbol.backingFieldSymbol!!
        }
    }

    private fun chooseMemberDeclarationIfNeeded(
        symbol: FirBasedSymbol<*>,
        session: LLFirResolveSession,
        directives: RegisteredDirectives,
    ): FirBasedSymbol<*> {
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

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    protected object Directives : SimpleDirectivesContainer() {
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

        val RESOLVE_PROPERTY_PART by enumDirective<PropertyPart>("Choose getter/setter/backing field in the case of property")
        val RESOLVE_SCRIPT by directive("Resolve script instead of declaration at caret")
        val RESOLVE_FILE by directive("Resolve file instead of declaration at caret")
        val RESOLVE_DANGLING_MODIFIER by directive("Resolve a file dangling modifier list instead of declaration at caret")

        val LAZY_MODE by enumDirective<LazyResolveMode>("Describes which lazy resolution call should be used")
    }

    /**
     * Describes which part of [org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol] should be chosen.
     */
    protected enum class PropertyPart {
        GETTER,
        SETTER,
        BACKING_FIELD;
    }

    protected enum class LazyResolveMode {
        /**
         * The default option.
         *
         * @see lazyResolveToPhase
         */
        Regular,

        /**
         * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
         */
        Recursive,

        /**
         * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
         */
        WithCallableMembers,
    }

    protected fun FirElementWithResolveState.lazyResolveToPhaseByDirective(toPhase: FirResolvePhase, testServices: TestServices) {
        when (testServices.moduleStructure.allDirectives.singleOrZeroValue(Directives.LAZY_MODE)) {
            LazyResolveMode.Regular, null -> lazyResolveToPhase(toPhase)
            LazyResolveMode.Recursive -> lazyResolveToPhaseRecursively(toPhase)
            LazyResolveMode.WithCallableMembers -> {
                if (this !is FirClass) {
                    error(
                        "Only ${FirClass::class.simpleName} can be used with ${LazyResolveMode.WithCallableMembers} mode, " +
                                "but ${this::class.simpleName} found"
                    )
                }

                lazyResolveToPhaseWithCallableMembers(toPhase)
            }
        }
    }
}

internal fun lazyResolveRenderer(builder: StringBuilder): FirRenderer = FirRenderer(
    builder = builder,
    declarationRenderer = FirDeclarationRendererWithFilteredAttributes(),
    resolvePhaseRenderer = FirResolvePhaseRenderer(),
    errorExpressionRenderer = FirErrorExpressionExtendedRenderer(),
)

internal operator fun List<FirFile>.contains(element: FirElementWithResolveState): Boolean = if (element is FirFile) {
    element in this
} else {
    any { file ->
        FirElementFinder.findElementIn<FirElementWithResolveState>(file) {
            it == element
        } != null
    }
}
