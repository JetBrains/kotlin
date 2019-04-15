/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider

fun createSession(
    environment: KotlinCoreEnvironment,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope = GlobalSearchScope.notScope(sourceScope)
): FirSession {
    val moduleInfo = FirTestModuleInfo()
    val project = environment.project
    val provider = FirProjectSessionProvider(project)
    return FirJavaModuleBasedSession(moduleInfo, provider, sourceScope).also {
        createSessionForDependencies(provider, moduleInfo, librariesScope, environment)
    }
}

private fun createSessionForDependencies(
    provider: FirProjectSessionProvider,
    moduleInfo: FirTestModuleInfo,
    librariesScope: GlobalSearchScope,
    environment: KotlinCoreEnvironment
) {
    val dependenciesInfo = FirTestModuleInfo()
    moduleInfo.dependencies.add(dependenciesInfo)
    FirLibrarySession.create(
        dependenciesInfo, provider, librariesScope, environment.project,
        environment.createPackagePartProvider(librariesScope)
    )
}
