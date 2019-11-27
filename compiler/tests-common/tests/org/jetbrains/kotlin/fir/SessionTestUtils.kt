/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

fun createSession(
    environment: KotlinCoreEnvironment,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope = GlobalSearchScope.notScope(sourceScope)
) = createSession(environment.project, sourceScope, librariesScope, environment::createPackagePartProvider)

fun createSession(
    project: Project,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
): FirSession {
    val moduleInfo = FirTestModuleInfo()
    val provider = FirProjectSessionProvider(project)
    return FirJavaModuleBasedSession(moduleInfo, provider, sourceScope).also {
        createSessionForDependencies(project, provider, moduleInfo, librariesScope, packagePartProvider)
    }
}

private fun createSessionForDependencies(
    project: Project,
    provider: FirProjectSessionProvider,
    moduleInfo: FirTestModuleInfo,
    librariesScope: GlobalSearchScope,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
) {
    val dependenciesInfo = FirTestModuleInfo()
    moduleInfo.dependencies.add(dependenciesInfo)
    FirLibrarySession.create(
        dependenciesInfo, provider, librariesScope, project, packagePartProvider(librariesScope)
    )
}
