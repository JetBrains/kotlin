/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.platform.declarations.createAnnotationResolver
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLCheckersFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirNonEmptyResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirExceptionHandler
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirExceptionHandler
import org.jetbrains.kotlin.fir.FirPrivateVisibleFromDifferentModuleExtension
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirHiddenDeprecationProvider
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
    register(FirElementFinder::class, FirElementFinder())
    register(FirPrivateVisibleFromDifferentModuleExtension::class, LLFirPrivateVisibleFromDifferentModuleExtension(this))
    register(
        FirLookupDefaultStarImportsInSourcesSettingHolder::class,
        createLookupDefaultStarImportsInSourcesSettingHolder(languageVersionSettings)
    )
    register(LLCheckersFactory::class, LLCheckersFactory(this))
    registerResolveExtensionTool()
    register(FirHiddenDeprecationProvider::class, LLHiddenDeprecationProvider(this))
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
internal fun FirSession.registerCompilerPluginExtensions(project: Project, module: KaModule) {
    FirSessionConfigurator(this).apply {
        this@apply.registerCompilerPluginExtensions(project, module)
    }.configure()
}

@SessionConfiguration
internal fun FirSessionConfigurator.registerCompilerPluginExtensions(project: Project, module: KaModule) {
    FirExtensionRegistrarAdapter.getInstances(project).forEach(::applyExtensionRegistrar)

    val pluginsProvider = KotlinCompilerPluginsProvider.getInstance(project) ?: return
    pluginsProvider
        .getRegisteredExtensions(module, FirExtensionRegistrarAdapter)
        .forEach(::applyExtensionRegistrar)
}

private fun FirSessionConfigurator.applyExtensionRegistrar(registrar: FirExtensionRegistrarAdapter) {
    registerExtensions((registrar as FirExtensionRegistrar).configure())
}

@SessionConfiguration
internal fun LLFirSession.registerCompilerPluginServices(
    project: Project,
    module: KaSourceModule
) {
    val projectWithDependenciesScope = KaResolutionScopeProvider.getInstance(project).getResolutionScope(module)
    val annotationsResolver = project.createAnnotationResolver(projectWithDependenciesScope)

    // We need FirRegisteredPluginAnnotations and FirPredicateBasedProvider during extensions' registration process
    register(FirRegisteredPluginAnnotations::class, LLFirIdeRegisteredPluginAnnotations(this, annotationsResolver))
    register(FirPredicateBasedProvider::class, LLFirIdePredicateBasedProvider(this, annotationsResolver))
}
