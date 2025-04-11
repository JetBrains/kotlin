/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirLanguageSettingsComponent
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.java.enhancement.AbstractJavaAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.fir.java.enhancement.FirEnhancedSymbolsStorage
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope
import org.jetbrains.kotlin.fir.types.TypeComponents
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

/**
 * Dummy implementation of AbstractJavaAnnotationTypeQualifierResolver for testing.
 */
class DummyFirAnnotationTypeQualifierResolver(
    session: FirSession,
    javaTypeEnhancementState: JavaTypeEnhancementState?,
) :
    AbstractJavaAnnotationTypeQualifierResolver(
        session,
        javaTypeEnhancementState ?: JavaTypeEnhancementState.Companion.getDefault(KotlinVersion(2, 1, 0))
    )
{
    override fun extractDefaultQualifiers(firClass: FirRegularClass): JavaTypeQualifiersByElementType? = null
}

/**
 * Creates a simple FirSession for testing.
 */
@OptIn(SessionConfiguration::class)
internal fun createTestSession(): FirSession {
    return object : FirSession(null, Kind.Source) {}.apply {
        val moduleData = object : FirModuleData() {
            override val name: Name = Name.identifier("<test module>")
            override val dependencies: List<FirModuleData> = emptyList()
            override val dependsOnDependencies: List<FirModuleData> = emptyList()
            override val allDependsOnDependencies: List<FirModuleData> = emptyList()
            override val friendDependencies: List<FirModuleData> = emptyList()
            override val platform = JvmPlatforms.unspecifiedJvmPlatform
            override val isCommon: Boolean = false
            override val session: FirSession
                get() = boundSession ?: error("Session not bound")
            override val stableModuleName: String? = null
        }

        register(FirModuleData::class, moduleData)
        moduleData.bindSession(this)
        register(FirCachesFactory::class, FirThreadUnsafeCachesFactory)
        register(FirLanguageSettingsComponent::class, FirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        register(TypeComponents::class, TypeComponents(this))
        register(FirExtensionService::class, FirExtensionService(this))
        val javaTypeEnhancementState = languageVersionSettings.getFlag(JvmAnalysisFlags.javaTypeEnhancementState)
        register(
            AbstractJavaAnnotationTypeQualifierResolver::class,
            DummyFirAnnotationTypeQualifierResolver(this, javaTypeEnhancementState)
        )
        register(FirEnhancedSymbolsStorage::class, FirEnhancedSymbolsStorage(this))
        register(JvmMappedScope.FirMappedSymbolStorage::class, JvmMappedScope.FirMappedSymbolStorage(this))
        val symbolProvider = FirCachingCompositeSymbolProvider(this, emptyList())
        register(FirSymbolProvider::class, symbolProvider)
        register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
    }
}
