/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirFirClassByPsiClassProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirLibrarySessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibrarySession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
class LLFirLibrarySessionFactory(
    private val project: Project,
) {

    internal fun getLibrarySession(ktBinaryModule: KtBinaryModule, sessionsCache: MutableMap<KtModule, LLFirSession>): LLFirLibrarySession {
        return sessionsCache.getOrPut(ktBinaryModule) {
            createModuleLibrariesSession(ktBinaryModule, sessionsCache)
        } as LLFirLibrarySession
    }

    private fun createModuleLibrariesSession(
        ktLibraryModule: KtBinaryModule,
        sessionsCache: MutableMap<KtModule, LLFirSession>,
    ): LLFirLibrarySession {
        val platform = ktLibraryModule.platform
        val builtinsSession = LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
        sessionsCache.putIfAbsent(builtinsSession.ktModule, builtinsSession)
        return LLFirLibrarySession(ktLibraryModule, project, builtinsSession.builtinTypes).apply session@{
            val moduleData = LLFirModuleData(ktLibraryModule).apply { bindSession(this@session) }
            registerModuleData(moduleData)
            registerIdeComponents(project)
            register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(LanguageVersionSettingsImpl.DEFAULT/*TODO*/)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerJavaSpecificResolveComponents()

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = LLFirLibraryProviderFactory.createLibraryProvidersForScope(
                this,
                moduleData,
                kotlinScopeProvider,
                project,
                builtinTypes,
                ktLibraryModule.contentScope,
                builtinsSession.symbolProvider
            )

            register(LLFirFirClassByPsiClassProvider::class, LLFirFirClassByPsiClassProvider(this))
            register(FirProvider::class, LLFirLibrarySessionProvider(symbolProvider))
            register(FirSymbolProvider::class, symbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirLibrarySessionFactory =
            project.getService(LLFirLibrarySessionFactory::class.java)
    }
}
