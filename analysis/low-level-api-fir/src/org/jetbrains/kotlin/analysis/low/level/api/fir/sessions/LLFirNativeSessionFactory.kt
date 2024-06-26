/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackagePartProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory.registerNativeComponents
import org.jetbrains.kotlin.utils.addIfNotNull

@OptIn(SessionConfiguration::class)
internal class LLFirNativeSessionFactory(project: Project) : LLFirAbstractSessionFactory(project) {
    override fun createSourcesSession(module: KaSourceModule): LLFirSourcesSession {
        return doCreateSourcesSession(module) { context ->
            registerNativeComponents()

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        context.firProvider.symbolProvider,
                        context.switchableExtensionDeclarationsSymbolProvider,
                        context.syntheticFunctionInterfaceProvider,
                    ),
                    context.dependencyProvider,
                )
            )
        }
    }

    override fun createLibrarySession(module: KaModule): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        return doCreateLibrarySession(module) { context ->
            registerNativeComponents()
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        context.firProvider.symbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )
        }
    }

    override fun createBinaryLibrarySession(module: KaLibraryModule): LLFirLibrarySession {
        return doCreateBinaryLibrarySession(module) {
            registerNativeComponents()
        }
    }

    override fun createDanglingFileSession(module: KaDanglingFileModule, contextSession: LLFirSession): LLFirSession {
        return doCreateDanglingFileSession(module, contextSession) {
            registerNativeComponents()

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        firProvider.symbolProvider,
                        switchableExtensionDeclarationsSymbolProvider,
                        syntheticFunctionInterfaceProvider,
                    ),
                    dependencyProvider,
                )
            )
        }
    }

    override fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val packagePartProvider = project.createPackagePartProvider(scope)
        return buildList {
            addAll(
                LLLibrarySymbolProviderFactory.fromSettings(project).createNativeLibrarySymbolProvider(
                    session,
                    moduleData,
                    kotlinScopeProvider,
                    moduleDataProvider,
                    scope,
                    isFallbackDependenciesProvider,
                )
            )
            addIfNotNull(
                OptionalAnnotationClassesProvider.createIfNeeded(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    packagePartProvider
                )
            )
        }
    }
}
