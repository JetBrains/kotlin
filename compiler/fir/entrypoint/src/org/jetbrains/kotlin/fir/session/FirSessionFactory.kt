/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.java.*
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.*
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

@OptIn(PrivateSessionConstructor::class)
object FirSessionFactory {
    fun createJavaModuleBasedSession(
        moduleInfo: ModuleInfo,
        sessionProvider: FirProjectSessionProvider,
        scope: GlobalSearchScope,
        dependenciesProvider: FirSymbolProvider? = null
    ): FirJavaModuleBasedSession {
        return FirJavaModuleBasedSession(moduleInfo, sessionProvider).apply {
            registerCommonComponents()
            registerResolveComponents()
            registerCheckersComponent()
            registerJavaSpecificComponents()

            val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)

            val firProvider = FirProviderImpl(this, kotlinScopeProvider)
            register(FirProvider::class, firProvider)

            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOf(
                        firProvider.symbolProvider,
                        JavaSymbolProvider(this, sessionProvider.project, scope),
                        dependenciesProvider ?: FirDependenciesSymbolProviderImpl(this)
                    )
                ) as FirSymbolProvider
            )

            Extensions.getArea(sessionProvider.project)
                .getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(FirJavaElementFinder(this, sessionProvider.project))
        }
    }

    fun createLibrarySession(
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

        val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(scope)
        return FirLibrarySession(moduleInfo, sessionProvider).apply {
            registerCommonComponents()

            val javaSymbolProvider = JavaSymbolProvider(this, sessionProvider.project, scope)

            val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)

            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOf(
                        KotlinDeserializedJvmSymbolsProvider(
                            this, sessionProvider.project,
                            packagePartProvider,
                            javaSymbolProvider,
                            kotlinClassFinder,
                            javaClassFinder,
                            kotlinScopeProvider
                        ),
                        FirBuiltinSymbolProvider(this, kotlinScopeProvider),
                        FirCloneableSymbolProvider(this, kotlinScopeProvider),
                        javaSymbolProvider,
                        FirDependenciesSymbolProviderImpl(this)
                    )
                )
            )
        }
    }

    @TestOnly
    fun createEmptySession(): FirSession {
        return object : FirSession(null) {}
    }
}
