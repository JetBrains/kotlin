/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.collections.forEach


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

internal fun findFirstDeclarationWithClass(
    declarations: List<KtDeclaration>,
    expectedClass: Class<out PsiElement>,
): KtDeclaration? {
    declarations.filterIsInstance(expectedClass).firstOrNull()?.let { return it as KtDeclaration }
    declarations.forEach { decl ->
        if (decl is KtDeclarationContainer) {
            findFirstDeclarationWithClass(decl.declarations, expectedClass)?.let { return it }
        }
        if (decl is KtFunction) {
            findFirstDeclarationWithClass(decl.valueParameters, expectedClass)?.let { return it }
        }
        if (decl is KtProperty) {
            findFirstDeclarationWithClass(decl.accessors, expectedClass)?.let { return it }
        }
        if (decl is KtTypeParameterListOwner) {
            findFirstDeclarationWithClass(decl.typeParameters, expectedClass)?.let { return it }
        }
        if (decl is KtClass && KtConstructor::class.java.isAssignableFrom(expectedClass)) {
            decl.primaryConstructor?.let { return it }
        }
    }
    return null
}

inline fun <reified E : FirElement> FirElement.collectAllElementsOfType(): List<E> {
    val result = mutableListOf<E>()
    this.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is E) result += element
            element.acceptChildren(this)
        }

    })

    return result
}

fun Collection<FirFile>.getDeclarationsToResolve() = flatMap { it.collectAllElementsOfType<FirDeclaration>() }.filterNot { declaration ->
    declaration is FirFile ||
            declaration is FirBackingField ||
            declaration is FirAnonymousFunction ||
            declaration is FirValueParameter && declaration.containingFunctionSymbol is FirAnonymousFunctionSymbol ||
            declaration is FirProperty && declaration.isLocal
}
