/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirDependenciesSymbolProviderImpl
import org.jetbrains.kotlin.fir.resolve.impl.FirLibrarySymbolProviderImpl
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

class FirJavaModuleBasedSession(
    moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider,
    scope: GlobalSearchScope,
    dependenciesProvider: FirSymbolProvider? = null
) : FirModuleBasedSession(moduleInfo, sessionProvider) {


    init {
        sessionProvider.sessionCache[moduleInfo] = this
        val firProvider = FirProviderImpl(this)
        registerComponent(FirProvider::class, firProvider)

        registerComponent(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                listOf(
                    service<FirProvider>(),
                    JavaSymbolProvider(this, sessionProvider.project, scope),
                    dependenciesProvider ?: FirDependenciesSymbolProviderImpl(this)
                )
            ) as FirSymbolProvider
        )

        registerComponent(
            FirCorrespondingSupertypesCache::class,
            FirCorrespondingSupertypesCache(this)
        )
    }
}

class FirLibrarySession private constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider,
    scope: GlobalSearchScope,
    packagePartProvider: PackagePartProvider,
    kotlinClassFinder: KotlinClassFinder,
    javaClassFinder: JavaClassFinder
) : FirSessionBase(sessionProvider) {


    init {
        sessionProvider.sessionCache[moduleInfo] = this
        val javaSymbolProvider = JavaSymbolProvider(this, sessionProvider.project, scope)

        registerComponent(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                listOf(
                    FirLibrarySymbolProviderImpl(this),
                    KotlinDeserializedJvmSymbolsProvider(
                        this, sessionProvider.project,
                        packagePartProvider,
                        javaSymbolProvider,
                        kotlinClassFinder,
                        javaClassFinder
                    ),
                    javaSymbolProvider,
                    FirDependenciesSymbolProviderImpl(this)
                )
            ) as FirSymbolProvider
        )
        registerComponent(FirClassDeclaredMemberScopeProvider::class, FirClassDeclaredMemberScopeProvider())

        registerComponent(
            FirCorrespondingSupertypesCache::class,
            FirCorrespondingSupertypesCache(this)
        )
    }

    companion object {
        fun create(
            moduleInfo: ModuleInfo,
            sessionProvider: FirProjectSessionProvider,
            scope: GlobalSearchScope,
            project: Project,
            packagePartProvider: PackagePartProvider
        ): FirLibrarySession {
            val javaClassFinder = JavaClassFinderImpl().apply {
                this.setProjectInstance(project)
                this.setScope(scope)
            }

            return FirLibrarySession(
                moduleInfo, sessionProvider, scope,
                packagePartProvider,
                VirtualFileFinderFactory.getInstance(project).create(scope),
                javaClassFinder
            )
        }
    }
}

class FirProjectSessionProvider(val project: Project) : FirSessionProvider {
    override fun getSession(moduleInfo: ModuleInfo): FirSession? {
        return sessionCache[moduleInfo]
    }

    val sessionCache = mutableMapOf<ModuleInfo, FirSession>()
}
