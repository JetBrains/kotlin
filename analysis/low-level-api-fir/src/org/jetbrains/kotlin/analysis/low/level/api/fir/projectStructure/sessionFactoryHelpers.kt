/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.platform.declarations.createAnnotationResolver
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirIdePredicateBasedProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirIdeRegisteredPluginAnnotations
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModulePrivateVisibilityChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.createLookupDefaultStarImportsInSourcesSettingHolder
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirNonEmptyResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirExceptionHandler
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirExceptionHandler
import org.jetbrains.kotlin.fir.FirModulePrivateVisibilityChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirLookupDefaultStarImportsInSourcesSettingHolder
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator

@SessionConfiguration
internal fun LLFirSession.registerIdeComponents(project: Project, languageVersionSettings: LanguageVersionSettings) {
    register(FirCachesFactory::class, FirThreadSafeCachesFactory(project))
    register(SealedClassInheritorsProvider::class, LLSealedInheritorsProvider(project))
    register(FirExceptionHandler::class, LLFirExceptionHandler)
    register(CodeFragmentScopeProvider::class, CodeFragmentScopeProvider(this))
    register(FirModulePrivateVisibilityChecker::class, LLFirModulePrivateVisibilityChecker(this))
    register(
        FirLookupDefaultStarImportsInSourcesSettingHolder::class,
        createLookupDefaultStarImportsInSourcesSettingHolder(languageVersionSettings)
    )
    registerResolveExtensionTool()
}

@SessionConfiguration
private fun LLFirSession.registerResolveExtensionTool() {
    val resolveExtensionTool = createResolveExtensionTool() ?: return

    // `KaResolveExtension`s are disposables meant to be tied to the lifetime of the `LLFirSession`.
    resolveExtensionTool.extensions.forEach { Disposer.register(requestDisposable(), it) }

    register(LLFirResolveExtensionTool::class, resolveExtensionTool)
}

private fun LLFirSession.createResolveExtensionTool(): LLFirResolveExtensionTool? {
    val extensions = KaResolveExtensionProvider.provideExtensionsFor(ktModule)
    if (extensions.isEmpty()) return null
    return LLFirNonEmptyResolveExtensionTool(this, extensions)
}


internal inline fun createCompositeSymbolProvider(
    session: FirSession,
    createSubProviders: MutableList<FirSymbolProvider>.() -> Unit
): FirCompositeSymbolProvider =
    FirCompositeSymbolProvider(session, buildList(createSubProviders))

@SessionConfiguration
internal fun FirSession.registerCompilerPluginExtensions(project: Project, module: KaSourceModule) {
    FirSessionConfigurator(this).apply {
        FirExtensionRegistrarAdapter.getInstances(project).forEach(::applyExtensionRegistrar)

        KotlinCompilerPluginsProvider.getInstance(project)
            ?.getRegisteredExtensions(module, FirExtensionRegistrarAdapter)
            ?.forEach(::applyExtensionRegistrar)
    }.configure()
}

private fun FirSessionConfigurator.applyExtensionRegistrar(registrar: FirExtensionRegistrarAdapter) {
    registerExtensions((registrar as FirExtensionRegistrar).configure())
}

@SessionConfiguration
internal fun LLFirSession.registerCompilerPluginServices(
    project: Project,
    module: KaSourceModule
) {
    val projectWithDependenciesScope = KotlinResolutionScopeProvider.getInstance(project).getResolutionScope(module)
    val annotationsResolver = project.createAnnotationResolver(projectWithDependenciesScope)

    // We need FirRegisteredPluginAnnotations and FirPredicateBasedProvider during extensions' registration process
    register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this, annotationsResolver))
    register(FirPredicateBasedProvider::class, LLFirIdePredicateBasedProvider(this, annotationsResolver))
}
