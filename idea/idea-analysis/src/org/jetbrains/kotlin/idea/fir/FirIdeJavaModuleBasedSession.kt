/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.CommonDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.jvm.JvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache


class FirIdeJavaModuleBasedSession(
    project: Project,
    moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider,
    scope: GlobalSearchScope
) : FirModuleBasedSession(moduleInfo, sessionProvider) {


    init {
        val firIdeProvider = FirIdeProvider(project, scope, this, KotlinScopeProvider(::wrapScopeWithJvmMapped))

        registerComponent(
            FirProvider::class,
            firIdeProvider
        )

        registerComponent(
            FirIdeProvider::class,
            firIdeProvider
        )

        registerComponent(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                listOf(
                    firProvider,
                    JavaSymbolProvider(this, sessionProvider.project, scope),
                    FirIdeModuleDependenciesSymbolProvider(this)
                )
            ) as FirSymbolProvider
        )

        registerComponent(
            FirCorrespondingSupertypesCache::class,
            FirCorrespondingSupertypesCache(this)
        )

        registerComponent(
            ConeCallConflictResolverFactory::class,
            JvmCallConflictResolverFactory
        )

        registerComponent(
            CheckersComponent::class,
            CheckersComponent.componentWithDefaultCheckers()
        )
    }
}