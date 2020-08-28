/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name

fun createSession(
    environment: KotlinCoreEnvironment,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope = GlobalSearchScope.notScope(sourceScope),
    moduleName: String = "TestModule"
): FirSession = createSession(environment.project, sourceScope, librariesScope, moduleName, environment::createPackagePartProvider)

fun createSession(
    project: Project,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    moduleName: String = "TestModule",
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
): FirSession {
    val moduleInfo = FirTestModuleInfo(name = Name.identifier(moduleName))
    val provider = FirProjectSessionProvider(project)
    return FirSessionFactory.createJavaModuleBasedSession(moduleInfo, provider, sourceScope).also {
        createSessionForDependencies(project, provider, moduleInfo, librariesScope, packagePartProvider)
        it.extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
    }
}

private fun createSessionForDependencies(
    project: Project,
    provider: FirProjectSessionProvider,
    moduleInfo: FirTestModuleInfo,
    librariesScope: GlobalSearchScope,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider
) {
    val dependenciesInfo = FirTestModuleInfo(name = Name.identifier(moduleInfo.name.identifier + ".dependencies"))
    moduleInfo.dependencies.add(dependenciesInfo)
    FirSessionFactory.createLibrarySession(
        dependenciesInfo, provider, librariesScope, project, packagePartProvider(librariesScope)
    )
}
