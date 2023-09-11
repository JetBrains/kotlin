/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveMultiDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder.findElementIn
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithFilteredAttributes
import org.jetbrains.kotlin.fir.renderer.FirErrorExpressionExtendedRenderer
import org.jetbrains.kotlin.fir.renderer.FirFileAnnotationsContainerRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Test that we do not resolve declarations we do not need & do not build bodies for them
 */
abstract class AbstractFirLazyDeclarationResolveTestCase : AbstractLowLevelApiSingleFileTest() {
    abstract fun checkSession(firSession: LLFirResolveSession)

    protected fun doLazyResolveTest(
        ktFile: KtFile,
        testServices: TestServices,
        resolverProvider: (LLFirResolveSession) -> Pair<FirElementWithResolveState, ((FirResolvePhase) -> Unit)>,
    ) {
        val resultBuilder = StringBuilder()
        val renderer = lazyResolveRenderer(resultBuilder)

        resolveWithClearCaches(ktFile) { firResolveSession ->
            checkSession(firResolveSession)

            val firFile = firResolveSession.getOrBuildFirFile(ktFile)
            val (elementToResolve, resolver) = resolverProvider(firResolveSession)
            val designations = LLFirResolveMultiDesignationCollector.getDesignationsToResolve(elementToResolve)
            val filesToRender = listOf(firFile).plus(designations.map { it.firFile }).distinct()
            val shouldRenderDeclaration = elementToResolve !is FirFile && filesToRender.all { file ->
                findElementIn<FirElementWithResolveState>(file) {
                    it == elementToResolve
                } == null
            }

            for (currentPhase in FirResolvePhase.entries) {
                if (currentPhase == FirResolvePhase.SEALED_CLASS_INHERITORS) continue
                resolver(currentPhase)

                if (resultBuilder.isNotEmpty()) {
                    resultBuilder.appendLine()
                }

                resultBuilder.append("${currentPhase.name}:")
                if (shouldRenderDeclaration) {
                    resultBuilder.append("\nTARGET: ")
                    renderer.renderElementAsString(elementToResolve)
                }

                for (file in filesToRender) {
                    resultBuilder.appendLine()
                    renderer.renderElementAsString(file)
                }
            }
        }

        resolveWithClearCaches(ktFile) { llSession ->
            checkSession(llSession)
            val firFile = llSession.getOrBuildFirFile(ktFile)
            firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
            if (resultBuilder.isNotEmpty()) {
                resultBuilder.appendLine()
            }

            resultBuilder.append("FILE RAW TO BODY:\n")
            renderer.renderElementAsString(firFile)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(resultBuilder.toString())
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
                symbol to symbol.fir.let { it.parameters + it.statements }.mapNotNull { (it as? FirDeclaration)?.symbol }
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
        val scope = classSymbol.unsubstitutedScope(
            session.useSiteFirSession,
            session.getScopeSessionFor(session.useSiteFirSession),
            false,
            FirResolvePhase.STATUS,
        )

        val names = scope.getCallableNames()
        val declarations = mutableListOf<FirBasedSymbol<*>>()
        for (name in names) {
            scope.processFunctionsByName(name) {
                if (filter(it)) {
                    declarations += it
                }
            }

            scope.processPropertiesByName(name) {
                if (filter(it)) {
                    declarations += it
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
    }
}

internal fun lazyResolveRenderer(builder: StringBuilder): FirRenderer = FirRenderer(
    builder = builder,
    declarationRenderer = FirDeclarationRendererWithFilteredAttributes(),
    resolvePhaseRenderer = FirResolvePhaseRenderer(),
    errorExpressionRenderer = FirErrorExpressionExtendedRenderer(),
    fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
)
