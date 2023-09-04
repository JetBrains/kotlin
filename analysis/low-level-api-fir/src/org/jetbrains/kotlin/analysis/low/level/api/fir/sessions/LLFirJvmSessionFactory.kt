/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.*
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createPackagePartProvider
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.OptionalAnnotationClassesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.utils.addIfNotNull

@OptIn(SessionConfiguration::class)
internal class LLFirJvmSessionFactory(project: Project) : LLFirAbstractSessionFactory(project) {

    override fun createSourcesSession(module: KtSourceModule): LLFirSourcesSession {
        return doCreateSourcesSession(module, FirKotlinScopeProvider(::wrapScopeWithJvmMapped)) { context ->
            registerJavaComponents(JavaModuleResolver.getInstance(project))
            val javaSymbolProvider = LLFirJavaSymbolProvider(this, context.moduleData, project, context.contentScope)
            register(JavaSymbolProvider::class, javaSymbolProvider)

            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        context.firProvider.symbolProvider,
                        context.switchableExtensionDeclarationsSymbolProvider,
                        javaSymbolProvider,
                        context.syntheticFunctionInterfaceProvider,
                    ),
                    context.dependencyProvider,
                )
            )

            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }

    override fun createLibrarySession(module: KtModule): LLFirLibraryOrLibrarySourceResolvableModuleSession {
        return doCreateLibrarySession(module) { context ->
            registerJavaComponents(JavaModuleResolver.getInstance(project))
            val javaSymbolProvider = LLFirJavaSymbolProvider(this, context.moduleData, project, context.contentScope)
            register(
                FirSymbolProvider::class,
                LLFirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        context.firProvider.symbolProvider,
                        javaSymbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )
            register(JavaSymbolProvider::class, javaSymbolProvider)
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }

    override fun createBinaryLibrarySession(module: KtBinaryModule): LLFirLibrarySession {
        return doCreateBinaryLibrarySession(module) {
            registerJavaComponents(JavaModuleResolver.getInstance(project))
            register(FirJvmTypeMapper::class, FirJvmTypeMapper(this))
        }
    }

    override fun createProjectLibraryProvidersForScope(
        session: LLFirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        project: Project,
        builtinTypes: BuiltinTypes,
        scope: GlobalSearchScope,
    ): List<FirSymbolProvider> {
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val packagePartProvider = project.createPackagePartProvider(scope)
        return buildList {
            val firJavaFacade = LLFirJavaFacadeForBinaries(session, builtinTypes, project.createJavaClassFinder(scope), moduleDataProvider)
            val deserializedSymbolProviderFactory = LLFirLibrarySymbolProviderFactory.getService(project)
            addAll(
                deserializedSymbolProviderFactory.createJvmLibrarySymbolProvider(
                    session,
                    moduleData,
                    kotlinScopeProvider,
                    moduleDataProvider,
                    firJavaFacade,
                    packagePartProvider,
                    scope
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
