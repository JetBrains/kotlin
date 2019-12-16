/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirDependenciesSymbolProviderImpl
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache


class FirIdeJavaModuleBasedSession(
    project: Project,
    moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider,
    scope: GlobalSearchScope
) : FirModuleBasedSession(moduleInfo, sessionProvider) {


    init {
        registerComponent(
            FirProvider::class,
            FirIdeProvider(project, scope, this)
        )
        registerComponent(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                listOf(
                    firProvider,
                    JavaSymbolProvider(this, sessionProvider.project, scope),
                    FirDependenciesSymbolProviderImpl(this)
                )
            ) as FirSymbolProvider
        )

        registerComponent(
            FirCorrespondingSupertypesCache::class,
            FirCorrespondingSupertypesCache(this)
        )
    }
}