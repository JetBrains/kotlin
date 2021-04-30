/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.nio.file.Path

@ObsoleteTestInfrastructure
fun createSessionForTests(
    environment: KotlinCoreEnvironment,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope = GlobalSearchScope.notScope(sourceScope),
    moduleName: String = "TestModule",
    friendsPaths: List<Path> = emptyList(),
): FirSession = createSessionForTests(
    environment.project,
    sourceScope,
    librariesScope,
    moduleName,
    friendsPaths,
    environment::createPackagePartProvider
)

@ObsoleteTestInfrastructure
fun createSessionForTests(
    project: Project,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    moduleName: String = "TestModule",
    friendsPaths: List<Path> = emptyList(),
    getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
): FirSession {
    return createSessionWithDependencies(
        Name.identifier(moduleName),
        JvmPlatforms.unspecifiedJvmPlatform,
        JvmPlatformAnalyzerServices,
        externalSessionProvider = null,
        project,
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        sourceScope,
        librariesScope,
        lookupTracker = null,
        getPackagePartProvider,
        getProviderAndScopeForIncrementalCompilation = { null },
        dependenciesConfigurator = {
            friendDependencies(friendsPaths)
        }
    )
}

