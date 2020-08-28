/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeSessionComponents
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

/**
 * [org.jetbrains.kotlin.fir.FirSession] responsible for all libraries analysing module transitively depends on
 */
@OptIn(PrivateSessionConstructor::class)
internal class FirIdeLibrariesSession private constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirIdeSessionProvider,
    override val scope: GlobalSearchScope
) : FirIdeSession(moduleInfo, sessionProvider) {
    companion object {
        fun create(
            moduleInfo: ModuleSourceInfo,
            sessionProvider: FirIdeSessionProvider,
            project: Project,
        ): FirIdeLibrariesSession {
            val searchScope = moduleInfo.module.moduleWithLibrariesScope
            val javaClassFinder = JavaClassFinderImpl().apply {
                setProjectInstance(project)
                setScope(searchScope)
            }
            val packagePartProvider = IDEPackagePartProvider(searchScope)

            val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope)
            return FirIdeLibrariesSession(moduleInfo, sessionProvider, searchScope).apply {
                registerCommonComponents()

                val javaSymbolProvider = JavaSymbolProvider(this, sessionProvider.project, searchScope)

                val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
                register(IdeSessionComponents::class, IdeSessionComponents.create(this))
                register(
                    FirSymbolProvider::class,
                    FirCompositeSymbolProvider(
                        this,
                        listOf(
                            KotlinDeserializedJvmSymbolsProvider(
                                this,
                                project,
                                packagePartProvider,
                                javaSymbolProvider,
                                kotlinClassFinder,
                                javaClassFinder,
                                kotlinScopeProvider
                            ),
                            FirBuiltinSymbolProvider(this, kotlinScopeProvider),
                            FirCloneableSymbolProvider(this, kotlinScopeProvider),
                            javaSymbolProvider,
                        )
                    )
                )
            }
        }
    }
}
