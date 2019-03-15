/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider

fun createSession(project: Project, sourceScope: GlobalSearchScope): FirSession {
    val moduleInfo = FirTestModuleInfo()
    val provider = FirProjectSessionProvider(project)
    return FirJavaModuleBasedSession(moduleInfo, provider, sourceScope).also {
        createSessionForDependencies(provider, moduleInfo, sourceScope)
    }
}

private fun createSessionForDependencies(
    provider: FirProjectSessionProvider, moduleInfo: FirTestModuleInfo, sourceScope: GlobalSearchScope
) {
    val dependenciesInfo = FirTestModuleInfo()
    moduleInfo.dependencies.add(dependenciesInfo)
    FirLibrarySession(dependenciesInfo, provider, GlobalSearchScope.notScope(sourceScope))
}
