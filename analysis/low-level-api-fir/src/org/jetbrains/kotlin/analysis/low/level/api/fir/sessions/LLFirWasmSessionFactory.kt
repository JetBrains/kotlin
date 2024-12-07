/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getWasmTarget
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.session.FirWasmSessionFactory.registerWasmComponents

@OptIn(SessionConfiguration::class)
internal class LLFirWasmSessionFactory(project: Project) : LLFirAbstractSessionFactory(project) {
    override fun createSourcesSession(module: KaSourceModule): LLFirSourcesSession {
        return doCreateSourcesSession(module) { context ->
            registerWasmComponents()

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
            registerWasmComponents()

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
            registerWasmComponents()
        }
    }

    override fun createDanglingFileSession(module: KaDanglingFileModule, contextSession: LLFirSession): LLFirSession {
        return doCreateDanglingFileSession(module, contextSession) { context ->
            registerWasmComponents()

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        firProvider.symbolProvider,
                        context.switchableExtensionDeclarationsSymbolProvider,
                        context.syntheticFunctionInterfaceProvider,
                    ),
                    context.dependencyProvider,
                )
            )
        }
    }

    private fun LLFirSession.registerWasmComponents() {
        val target = ktModule.targetPlatform.getWasmTarget()
        registerWasmComponents(target)
    }

    override fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        scope: GlobalSearchScope,
        isFallbackDependenciesProvider: Boolean,
    ): List<FirSymbolProvider> {
        return LLLibrarySymbolProviderFactory.fromSettings(project).createWasmLibrarySymbolProvider(
            session,
            scope,
            isFallbackDependenciesProvider,
        )
    }
}
