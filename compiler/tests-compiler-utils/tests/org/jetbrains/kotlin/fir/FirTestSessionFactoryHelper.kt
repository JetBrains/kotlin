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
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.nio.file.Path

object FirTestSessionFactoryHelper {
    @ObsoleteTestInfrastructure
    fun createSessionForTests(
        projectEnvironment: AbstractProjectEnvironment,
        javaSourceScope: AbstractProjectFileSearchScope,
        librariesScope: AbstractProjectFileSearchScope = !javaSourceScope,
        moduleName: String = "TestModule",
        friendsPaths: List<Path> = emptyList(),
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
    ): FirSession = FirSessionFactoryHelper.createSessionWithDependencies(
        Name.identifier(moduleName),
        JvmPlatforms.unspecifiedJvmPlatform,
        JvmPlatformAnalyzerServices,
        externalSessionProvider = null,
        projectEnvironment,
        languageVersionSettings,
        javaSourceScope,
        librariesScope,
        lookupTracker = null,
        enumWhenTracker = null,
        incrementalCompilationContext = null,
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
        return FirSessionFactoryHelper.createSessionWithDependencies(
            Name.identifier(moduleName),
            JvmPlatforms.unspecifiedJvmPlatform,
            JvmPlatformAnalyzerServices,
            externalSessionProvider = null,
            VfsBasedProjectEnvironment(
                project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                getPackagePartProvider
            ),
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            PsiBasedProjectFileSearchScope(sourceScope),
            PsiBasedProjectFileSearchScope(librariesScope),
            lookupTracker = null,
            enumWhenTracker = null,
            incrementalCompilationContext = null,
            extensionRegistrars = emptyList(),
            needRegisterJavaElementFinder = true,
            dependenciesConfigurator = {
                friendDependencies(friendsPaths)
            }
        )
    }
}