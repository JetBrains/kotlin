/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.ignoreExceptionIfIgnoreDirectivePresent
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test exists to check lazy resolution for fake overrides
 */
abstract class AbstractLazyDeclarationResolveScopeBasedTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val scopeOwner = testServices.moduleStructure.allDirectives.singleOrZeroValue(Directives.SCOPE_OWNER)
        val classOrObject = if (scopeOwner == null) {
            testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtClassOrObject>(mainFile)
        } else {
            null
        }

        testServices.moduleStructure.allDirectives.ignoreExceptionIfIgnoreDirectivePresent(Directives.IGNORE_SCOPE_BASED_RESOLVE) {
            doTest(mainFile, classOrObject, scopeOwner, testServices)
        }
    }

    private fun doTest(mainFile: KtFile, classOrObject: KtClassOrObject?, scopeOwner: String?, testServices: TestServices) {
        withResolutionFacade(classOrObject ?: mainFile) { resolutionFacade ->
            prewarmScopesWithoutRequiredPhase(resolutionFacade, testServices)

            val classSymbol = classOrObject?.resolveToFirSymbolOfType<FirClassSymbol<*>>(resolutionFacade)
                ?: findClassSymbol(resolutionFacade, scopeOwner!!)

            val symbols = collectAllCallableDeclarations(classSymbol, resolutionFacade)
            val dumpBefore = dumpSymbols(symbols)
            testServices.assertions.assertEqualsToTestOutputFile(dumpBefore, extension = "before.txt")
            for (callableSymbol in symbols) {
                callableSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            }

            val dumpAfter = dumpSymbols(symbols)
            testServices.assertions.assertEqualsToTestOutputFile(dumpAfter, extension = "after.txt")
        }
    }

    /**
     * Builds use-site scopes without a required member phase, as some compiler clients do, so the scope session
     * already contains them when the test requests the scope with the [STATUS][FirResolvePhase.STATUS] phase.
     */
    private fun prewarmScopesWithoutRequiredPhase(resolutionFacade: LLResolutionFacade, testServices: TestServices) {
        val session = resolutionFacade.useSiteFirSession
        val scopeSession = resolutionFacade.getScopeSessionFor(session)
        for (classId in testServices.moduleStructure.allDirectives[Directives.PREWARM_SCOPES_WITHOUT_REQUIRED_PHASE]) {
            findClassSymbol(resolutionFacade, classId)
                .unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
        }
    }

    private fun findClassSymbol(resolutionFacade: LLResolutionFacade, classId: String): FirClassSymbol<*> {
        val symbol = resolutionFacade.useSiteFirSession.symbolProvider.getClassLikeSymbolByClassId(ClassId.fromString(classId))
        return symbol as? FirClassSymbol<*> ?: error("Class '$classId' is not found")
    }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_SCOPE_BASED_RESOLVE by stringDirective("Temporary disable test until the issue is fixed")

        val SCOPE_OWNER by stringDirective("Class id of the scope owner, if it cannot be marked with a caret (e.g., a Java class)")

        val PREWARM_SCOPES_WITHOUT_REQUIRED_PHASE by stringDirective(
            "Class ids whose use-site scopes are built without a required member phase before the test",
        )
    }
}

private fun collectAllCallableDeclarations(
    classSymbol: FirClassSymbol<*>,
    resolutionFacade: LLResolutionFacade
): Collection<FirCallableSymbol<*>> {
    val baseScope = classSymbol.unsubstitutedScope(
        resolutionFacade.useSiteFirSession,
        resolutionFacade.getScopeSessionFor(resolutionFacade.useSiteFirSession),
        false,
        FirResolvePhase.STATUS,
    )

    return buildSet {
        baseScope.processAllCallables { callable ->
            add(callable)
            @OptIn(ScopeFunctionRequiresPrewarm::class)
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

abstract class AbstractSourceLikeLazyDeclarationResolveScopeBasedTest : AbstractLazyDeclarationResolveScopeBasedTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}

abstract class AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest : AbstractLazyDeclarationResolveScopeBasedTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}
