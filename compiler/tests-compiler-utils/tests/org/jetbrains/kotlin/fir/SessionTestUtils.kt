/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.nio.file.Path

@ObsoleteTestInfrastructure
fun createSessionForTests(
    projectEnvironment: AbstractProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    librariesScope: AbstractProjectFileSearchScope = !sourceScope,
    moduleName: String = "TestModule",
    friendsPaths: List<Path> = emptyList(),
    languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
): FirSession = FirSessionFactory.createSessionWithDependencies(
    Name.identifier(moduleName),
    JvmPlatforms.unspecifiedJvmPlatform,
    JvmPlatformAnalyzerServices,
    externalSessionProvider = null,
    projectEnvironment,
    languageVersionSettings,
    sourceScope,
    librariesScope,
    lookupTracker = null,
    providerAndScopeForIncrementalCompilation = null,
    extensionRegistrars = emptyList(),
    needRegisterJavaElementFinder = true,
    dependenciesConfigurator = {
        friendDependencies(friendsPaths)
    }
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
    return FirSessionFactory.createSessionWithDependencies(
        Name.identifier(moduleName),
        JvmPlatforms.unspecifiedJvmPlatform,
        JvmPlatformAnalyzerServices,
        externalSessionProvider = null,
        PsiBasedProjectEnvironment(project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL), getPackagePartProvider),
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        PsiBasedProjectFileSearchScope(sourceScope),
        PsiBasedProjectFileSearchScope(librariesScope),
        lookupTracker = null,
        providerAndScopeForIncrementalCompilation = null,
        extensionRegistrars = emptyList(),
        needRegisterJavaElementFinder = true,
        dependenciesConfigurator = {
            friendDependencies(friendsPaths)
        }
    )
}

