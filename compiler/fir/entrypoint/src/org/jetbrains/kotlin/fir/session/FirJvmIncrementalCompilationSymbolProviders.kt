/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilationComponents
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.modules.TargetId

data class FirJvmIncrementalCompilationSymbolProviders(
    val symbolProviderForBinariesFromIncrementalCompilation: FirSymbolProvider?,
    val optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation: OptionalAnnotationClassesProvider?,
)

fun IncrementalCompilationContext.createSymbolProviders(
    session: FirSession,
    moduleData: FirModuleData,
    projectEnvironment: AbstractProjectEnvironment,
): FirJvmIncrementalCompilationSymbolProviders {
    var symbolProviderForBinariesFromIncrementalCompilation: JvmClassFileBasedSymbolProvider? = null
    var optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation: OptionalAnnotationClassesProvider? = null
    if (precompiledBinariesFileScope != null) {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kotlinScopeProvider = session.kotlinScopeProvider
        symbolProviderForBinariesFromIncrementalCompilation =
            JvmClassFileBasedSymbolProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                precompiledBinariesPackagePartProvider,
                projectEnvironment.getKotlinClassFinder(precompiledBinariesFileScope),
                projectEnvironment.getFirJavaFacade(session, moduleData, precompiledBinariesFileScope),
                defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled
            )
        optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation =
            OptionalAnnotationClassesProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                precompiledBinariesPackagePartProvider,
                defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled
            )
    }
    return FirJvmIncrementalCompilationSymbolProviders(
        symbolProviderForBinariesFromIncrementalCompilation,
        optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation
    )
}

fun createIncrementalProvidersForNonLeafMppModules(
    session: FirSession,
    moduleData: FirModuleData,
    configuration: CompilerConfiguration,
): FirJvmIncrementalCompilationSymbolProviders? {
    val moduleName = moduleData.name.asStringStripSpecialMarkers()
    val incrementalCache = configuration.incrementalCacheForThisTarget() ?: return null

    val provider = KlibIcCacheBasedSymbolProvider(
        session = session,
        moduleDataProvider = SingleModuleDataProvider(moduleData),
        kotlinScopeProvider = session.kotlinScopeProvider,
        icData = KlibIcData(incrementalCache.getMetadata(moduleName)),
        defaultDeserializationOrigin = FirDeclarationOrigin.Precompiled,
    )
    return FirJvmIncrementalCompilationSymbolProviders(
        symbolProviderForBinariesFromIncrementalCompilation = provider,
        optionalAnnotationClassesProviderForBinariesFromIncrementalCompilation = null,
    )
}

private fun CompilerConfiguration.incrementalCacheForThisTarget(): IncrementalCache? {
    val moduleName = requireNotNull(moduleName) { "Module name must be specified for incremental compilation" }
    val targetId = TargetId(moduleName, "java-production")

    return incrementalCompilationComponents?.getIncrementalCache(targetId)
}
