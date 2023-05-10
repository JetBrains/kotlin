/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirBuiltinsAndCloneableSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirBuiltinsAndCloneableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmStubBasedFirDeserializedSymbolProvider
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirExtensionSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirDummyCompilerLazyDeclarationResolver
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerCommonComponentsAfterExtensionsAreConfigured
import org.jetbrains.kotlin.fir.session.registerCommonJavaComponents
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.util.concurrent.ConcurrentHashMap

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
class LLFirBuiltinsSessionFactory(private val project: Project) {
    private val builtInTypes = BuiltinTypes() // TODO should be platform-specific

    private val builtinsModules = ConcurrentHashMap<TargetPlatform, KtBuiltinsModule>()

    private val builtinsAndCloneableSessions = ConcurrentHashMap<TargetPlatform, LLFirBuiltinsAndCloneableSession>()

    /**
     * Returns the [platform]'s [KtBuiltinsModule]. [getBuiltinsModule] should be used instead of [getBuiltinsSession] when a
     * [KtBuiltinsModule] is needed as a dependency for other [KtModule]s. This is because during project structure creation, we have to
     * avoid the creation of the builtins *session*, as not all services might have been registered at that point.
     */
    fun getBuiltinsModule(platform: TargetPlatform): KtBuiltinsModule =
        builtinsModules.getOrPut(platform) { KtBuiltinsModule(platform, platform.getAnalyzerServices(), project) }

    fun getBuiltinsSession(platform: TargetPlatform): LLFirBuiltinsAndCloneableSession =
        builtinsAndCloneableSessions.getOrPut(platform) { createBuiltinsAndCloneableSession(platform) }

    @TestOnly
    fun clearForTheNextTest() {
        builtinsModules.clear()
        builtinsAndCloneableSessions.clear()
    }

    private fun createBuiltinsAndCloneableSession(platform: TargetPlatform): LLFirBuiltinsAndCloneableSession {
        val builtinsModule = getBuiltinsModule(platform)

        val session = LLFirBuiltinsAndCloneableSession(builtinsModule, ModificationTracker.NEVER_CHANGED, builtInTypes)
        val moduleData = LLFirModuleData(builtinsModule).apply { bindSession(session) }

        return session.apply {
            registerIdeComponents(project)
            register(FirLazyDeclarationResolver::class, FirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(LanguageVersionSettingsImpl.DEFAULT/*TODO*/)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerCommonJavaComponents(JavaModuleResolver.getInstance(project))
            registerModuleData(moduleData)

            val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(FirKotlinScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = createCompositeSymbolProvider(this) {
                val moduleDataProvider = SingleModuleDataProvider(moduleData)
                add(
                    object : JvmStubBasedFirDeserializedSymbolProvider(
                        session,
                        moduleDataProvider,
                        kotlinScopeProvider,
                        project,
                        BuiltinsGlobalSearchScope(project),
                        FirDeclarationOrigin.BuiltIns
                    ) {
                        private val syntheticFunctionInterfaceProvider = FirBuiltinSyntheticFunctionInterfaceProvider(
                            session,
                            moduleData,
                            kotlinScopeProvider
                        )

                        override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
                            return super.getClassLikeSymbolByClassId(classId)
                                ?: syntheticFunctionInterfaceProvider.getClassLikeSymbolByClassId(classId)
                        }
                    }
                )
                add(FirExtensionSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider))
                add(FirCloneableSymbolProvider(session, moduleData, kotlinScopeProvider))
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

internal class BuiltinsGlobalSearchScope(project: Project) : DelegatingGlobalSearchScope(project, allScope(project)) {
    override fun contains(file: VirtualFile): Boolean {
        if (file.extension != BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
            return false
        }
        return super.contains(file)
    }
}

private fun TargetPlatform.getAnalyzerServices() = when {
    isJvm() -> JvmPlatformAnalyzerServices
    isJs() -> JvmPlatformAnalyzerServices/*TODO*/
//    isNative() -> NativePlatformAnalyzerServices
    isCommon() -> CommonPlatformAnalyzerServices
    else -> JvmPlatformAnalyzerServices/*TODO*/
}
