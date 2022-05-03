/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirBuiltinSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirBuiltinsAndCloneableSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirLibrariesSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibrariesSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseCheckingPhaseManager
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerCommonJavaComponents
import org.jetbrains.kotlin.fir.session.registerJavaSpecificResolveComponents
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.util.concurrent.ConcurrentHashMap

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal class LLFirLibrarySessionFactory(
    private val project: Project,
    private val useSiteModule: KtModule,
    private val useSiteLanguageVersionSettings: LanguageVersionSettings,
) {
    val builtInTypes = BuiltinTypes()
    val builtinsAndCloneableSession = createBuiltinsAndCloneableSession()

    private val librarySessionByModule = ConcurrentHashMap<KtModule, LLFirLibrariesSession>()

    fun getLibrarySessionForSourceModule(ktModule: KtSourceModule): LLFirLibrariesSession {
        return librarySessionByModule.getOrPut(ktModule) { createModuleLibrariesSession(ktModule) }
    }

    private fun createModuleLibrariesSession(
        sourceModule: KtSourceModule,
    ): LLFirLibrariesSession {
        return LLFirLibrariesSession(project, builtInTypes).apply session@{
            registerModuleData(LLFirKtModuleBasedModuleData(sourceModule).apply { bindSession(this@session) })
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(sourceModule.languageVersionSettings)
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val providers = LLFirLibraryProviderFactory.createProvidersByModuleLibraryDependencies(
                this,
                sourceModule,
                kotlinScopeProvider,
                project,
                builtinTypes
            ) { binaryDependencies ->
                GlobalSearchScope.union(binaryDependencies.map { it.contentScope })
            }

            val symbolProvider = createCompositeSymbolProvider(this) {
                addAll(providers)
                add(builtinsAndCloneableSession.symbolProvider)
            }

            register(FirProvider::class, LLFirLibrariesSessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }


    private fun createBuiltinsAndCloneableSession(): LLFirBuiltinsAndCloneableSession {
        return LLFirBuiltinsAndCloneableSession(project, builtInTypes).apply session@{
            val moduleData = LLFirBuiltinsModuleData(useSiteModule).apply {
                bindSession(this@session)
            }
            registerIdeComponents(project)
            register(FirPhaseManager::class, FirPhaseCheckingPhaseManager)
            registerCommonComponents(useSiteLanguageVersionSettings)
            registerModuleData(moduleData)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)
            val symbolProvider = createCompositeSymbolProvider(this) {
                add(LLFirBuiltinSymbolProvider(this@session, moduleData, kotlinScopeProvider))
                add(FirCloneableSymbolProvider(this@session, moduleData, kotlinScopeProvider))
            }

            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, LLFirBuiltinsAndCloneableSessionProvider(symbolProvider))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }
}