/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirDependentModuleProvidersBySessions
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirNonUnderContentRootSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidator
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.utils.caches.SoftCachedMap
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.extensions.FirEmptyPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

internal class LLFirNonUnderContentRootSessionFactory(private val project: Project) {
    private val cache = SoftCachedMap.create<KtNotUnderContentRootModule, LLFirResolvableModuleSession>(
        project,
        kind = SoftCachedMap.Kind.STRONG_KEYS_SOFT_VALUES,
        trackers = listOf(PsiModificationTracker.MODIFICATION_COUNT),
    )

    fun getNonUnderContentRootSession(
        module: KtNotUnderContentRootModule,
    ): LLFirResolvableModuleSession {
        return cache.getOrPut(module) { createSession(module, LLFirSessionInvalidator { cache.clear() }) }
    }

    @OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
    private fun createSession(
        module: KtNotUnderContentRootModule,
        sessionInvalidator: LLFirSessionInvalidator
    ): LLFirResolvableModuleSession {
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(JvmPlatforms.unspecifiedJvmPlatform)
        val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        val scopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        val globalResolveComponents = LLFirGlobalResolveComponents(project)
        val components = LLFirModuleResolveComponents(module, globalResolveComponents, scopeProvider, sessionInvalidator)
        val contentScope = module.contentScope
        val session = LLFirNonUnderContentRootSession(module, project, components, builtinsSession.builtinTypes)
        components.session = session

        return session.apply session@{
            val moduleData = LLFirModuleData(module).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            register(FirKotlinScopeProvider::class, scopeProvider)

            registerIdeComponents(project)
            registerCommonComponents(languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerResolveComponents()
            registerJavaSpecificResolveComponents()

            val provider = LLFirProvider(
                this,
                components,
                project.createDeclarationProvider(contentScope),
                project.createPackageProvider(contentScope),
                canContainKotlinPackage = true,
            )

            register(FirProvider::class, provider)
            register(FirLazyDeclarationResolver::class, LLFirLazyDeclarationResolver())

            val dependencyProvider = LLFirDependentModuleProvidersBySessions(this) {
                add(builtinsSession)
            }

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    dependencyProvider,
                    providers = listOfNotNull(
                        provider.symbolProvider,
                    )
                )
            )

            register(FirPredicateBasedProvider::class, FirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
            register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.Empty)
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirNonUnderContentRootSessionFactory =
            project.getService(LLFirNonUnderContentRootSessionFactory::class.java)
    }
}

