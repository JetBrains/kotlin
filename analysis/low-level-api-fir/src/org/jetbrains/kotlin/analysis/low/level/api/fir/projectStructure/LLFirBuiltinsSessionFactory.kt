/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBuiltinsModuleImpl
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirBuiltinsAndCloneableSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerCommonComponentsAfterExtensionsAreConfigured
import org.jetbrains.kotlin.fir.session.registerJavaComponents
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.util.concurrent.ConcurrentHashMap

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
@LLFirInternals
class LLFirBuiltinsSessionFactory(private val project: Project) {
    private val builtInTypes = BuiltinTypes() // TODO should be platform-specific

    private val builtinsModules = ConcurrentHashMap<TargetPlatform, KaBuiltinsModule>()

    private val builtinsAndCloneableSessions = ConcurrentHashMap<TargetPlatform, CachedValue<LLFirBuiltinsAndCloneableSession>>()

    /**
     * Returns the [platform]'s [KaBuiltinsModule]. [getBuiltinsModule] should be used instead of [getBuiltinsSession] when a
     * [KaBuiltinsModule] is needed as a dependency for other [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule]s. This
     * is because during project structure creation, we have to avoid the creation of the builtins *session*, as not all services might have
     * been registered at that point.
     */
    fun getBuiltinsModule(platform: TargetPlatform): KaBuiltinsModule =
        builtinsModules.getOrPut(platform) { KaBuiltinsModuleImpl(platform, project) }

    fun getBuiltinsSession(platform: TargetPlatform): LLFirBuiltinsAndCloneableSession =
        builtinsAndCloneableSessions.getOrPut(platform) {
            CachedValuesManager.getManager(project).createCachedValue {
                val session = createBuiltinsAndCloneableSession(platform)
                CachedValueProvider.Result(session, session.createValidityTracker())
            }
        }.value

    @TestOnly
    fun clearForTheNextTest() {
        builtinsModules.clear()
        builtinsAndCloneableSessions.clear()
    }

    private fun createBuiltinsAndCloneableSession(platform: TargetPlatform): LLFirBuiltinsAndCloneableSession {
        val builtinsModule = getBuiltinsModule(platform)

        val session = LLFirBuiltinsAndCloneableSession(builtinsModule, builtInTypes)
        val moduleData = LLFirModuleData(session)

        return session.apply {
            val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
            registerIdeComponents(project, languageVersionSettings)
            register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerModuleData(moduleData)

            if (platform.isJvm()) {
                registerJavaComponents(JavaModuleResolver.getInstance(project))
            }

            val kotlinScopeProvider = when {
                platform.isJvm() -> FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
                else -> FirKotlinScopeProvider()
            }
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = createCompositeSymbolProvider(this) {
                addAll(
                    LLLibrarySymbolProviderFactory
                        .fromSettings(project)
                        .createBuiltinsSymbolProvider(session)
                )

                add(FirExtensionSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider))
                if (platform.has<JvmPlatform>()) {
                    add(FirCloneableSymbolProvider(session, moduleData, kotlinScopeProvider))
                }
            }

            register(FirSymbolProvider::class, symbolProvider)
            register(FirProvider::class, LLFirBuiltinsAndCloneableSessionProvider(symbolProvider))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirBuiltinsSessionFactory =
            project.getService(LLFirBuiltinsSessionFactory::class.java)
    }
}
