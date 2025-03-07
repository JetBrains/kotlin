/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.FirPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.PlatformConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeCastChecker
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeConflictDeclarationsDiagnosticDispatcher
import org.jetbrains.kotlin.fir.backend.native.FirNativeClassMapper
import org.jetbrains.kotlin.fir.backend.native.FirNativeOverrideChecker
import org.jetbrains.kotlin.fir.checkers.registerExtraNativeCheckers
import org.jetbrains.kotlin.fir.checkers.registerNativeCheckers
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirDefaultImportProviderHolder
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices

@OptIn(SessionConfiguration::class)
object FirNativeSessionFactory : AbstractFirKlibSessionFactory<Nothing?, Nothing?>() {

    // ==================================== Library session ====================================

    override fun createLibraryContext(configuration: CompilerConfiguration): Nothing? {
        return null
    }

    override fun createAdditionalDependencyProviders(
        session: FirSession,
        moduleDataProvider: ModuleDataProvider,
        kotlinScopeProvider: FirKotlinScopeProvider,
        resolvedLibraries: List<KotlinLibrary>,
    ): List<FirSymbolProvider> {
        val forwardDeclarationsModuleData = BinaryModuleData.createDependencyModuleData(FORWARD_DECLARATIONS_MODULE_NAME).apply {
            bindSession(session)
        }
        val provider = NativeForwardDeclarationsSymbolProvider(
            session,
            forwardDeclarationsModuleData,
            kotlinScopeProvider,
            resolvedLibraries
        )
        return listOf(provider)
    }

    override fun FirSession.registerLibrarySessionComponents(c: Nothing?) {
        registerComponents()
    }

    // ==================================== Platform session ====================================

    override fun FirSessionConfigurator.registerPlatformCheckers(c: Nothing?) {
        registerNativeCheckers()
    }

    override fun FirSessionConfigurator.registerExtraPlatformCheckers(c: Nothing?) {
        registerExtraNativeCheckers()
    }

    override fun FirSession.registerSourceSessionComponents(c: Nothing?) {
        registerComponents()
    }

    override fun createSourceContext(configuration: CompilerConfiguration): Nothing? {
        return null
    }

    // ==================================== Common parts ====================================

    private fun FirSession.registerComponents() {
        registerDefaultComponents()
        registerNativeComponents()
    }

    // ==================================== Utilities ====================================

    fun FirSession.registerNativeComponents() {
        register(FirPlatformClassMapper::class, FirNativeClassMapper())
        register(FirPlatformSpecificCastChecker::class, FirNativeCastChecker)
        register(PlatformConflictDeclarationsDiagnosticDispatcher::class, NativeConflictDeclarationsDiagnosticDispatcher)
        register(FirOverrideChecker::class, FirNativeOverrideChecker(this))
        register(FirDefaultImportProviderHolder::class, FirDefaultImportProviderHolder(NativePlatformAnalyzerServices))
    }
}
