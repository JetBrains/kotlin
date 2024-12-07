/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices


internal fun FirElement.renderWithClassName(): String =
    "${this::class.simpleName} `${render()}`"


internal fun FirBasedSymbol<*>.name(): String = when (this) {
    is FirCallableSymbol<*> -> callableId.callableName.asString()
    is FirClassLikeSymbol<*> -> classId.shortClassName.asString()
    is FirAnonymousInitializerSymbol -> "<init>"
    is FirFileSymbol -> "<FILE>"
    else -> error("unknown symbol ${this::class.simpleName}")
}

internal fun FirDeclaration.name(): String = symbol.name()

internal inline fun <R> resolveWithClearCaches(context: KtElement, action: (LLFirResolveSession) -> R): R {
    val project = context.project
    val module = KotlinProjectStructureProvider.getModule(project, context, useSiteModule = null)
    val resolveSession = LLFirResolveSessionService.getInstance(project).getFirResolveSessionNoCaching(module)
    return action(resolveSession)
}

internal inline fun <R> resolveWithCaches(context: KtElement, action: (LLFirResolveSession) -> R): R {
    val project = context.project
    val module = KotlinProjectStructureProvider.getModule(project, context, useSiteModule = null)
    val resolveSession = LLFirResolveSessionService.getInstance(project).getFirResolveSession(module)
    return action(resolveSession)
}

internal val LLFirResolveSession.isSourceSession: Boolean
    get() = when (useSiteKtModule) {
        is KaLibraryModule, is KaLibrarySourceModule -> false
        else -> true
    }

internal fun TestConfigurationBuilder.useFirSessionConfigurator(configurator: (TestServices) -> LLFirSessionConfigurator) {
    class ConfiguratorPreAnalysisHandler(testServices: TestServices) : PreAnalysisHandler(testServices) {
        override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
            val project = testServices.environmentManager.getProject()
            LLFirSessionConfigurator.registerExtension(project, configurator(testServices))
        }
    }

    usePreAnalysisHandlers(::ConfiguratorPreAnalysisHandler)
}

internal inline fun <reified E : FirElement> FirElement.collectAllElementsOfType(): List<E> {
    val result = mutableListOf<E>()
    this.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is E) result += element
            element.acceptChildren(this)
        }

    })

    return result
}

/**
 * @see canBeResolved
 */
internal fun Collection<FirFile>.getDeclarationsToResolve(): List<FirDeclaration> = flatMap {
    it.collectAllElementsOfType<FirDeclaration>()
}.filter(FirDeclaration::canBeResolved)

/**
 * [org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase] doesn't work for local declarations,
 * so such declarations may still have [FirResolvePhase.RAW_FIR] after lazy resolve call.
 *
 * All local declarations are not available during [FirResolvePhase.RAW_FIR] as we build bodies
 * lazily, but this is not the case for the last script statement due to the implementation details.
 * In this case, we may have local declarations, and currently this list is not complete, but it is enough
 * to pass all tests.
 */
private val FirDeclaration.canBeResolved: Boolean
    get() = when (this) {
        is FirAnonymousFunction -> false
        is FirProperty -> !isLocal
        is FirValueParameter -> containingDeclarationSymbol.fir.canBeResolved
        is FirPropertyAccessor -> propertySymbol.fir.canBeResolved
        is FirBackingField -> propertySymbol.fir.canBeResolved
        else -> true
    }
