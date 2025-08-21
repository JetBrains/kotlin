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
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

object FirTestSessionFactoryHelper {
    @ObsoleteTestInfrastructure
    fun createSessionForTests(
        projectEnvironment: VfsBasedProjectEnvironment,
        javaSourceScope: AbstractProjectFileSearchScope,
        librariesScope: AbstractProjectFileSearchScope = !javaSourceScope,
        moduleName: String = "TestModule",
        friendsPaths: List<Path> = emptyList(),
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
    ): FirSession {
        val configuration = CompilerConfiguration().apply {
            this.languageVersionSettings = languageVersionSettings
        }
        return FirSessionFactoryHelper.createSessionWithDependencies(
            Name.identifier(moduleName),
            JvmPlatforms.unspecifiedJvmPlatform,
            projectEnvironment,
            configuration,
            javaSourceScope,
            librariesScope,
            incrementalCompilationContext = null,
            extensionRegistrars = emptyList(),
            needRegisterJavaElementFinder = true,
            dependenciesConfigurator = {
                friendDependencies(friendsPaths.map { it.pathString })
            }
        )
    }

    @ObsoleteTestInfrastructure
    fun createSessionForTests(
        project: Project,
        sourceScope: GlobalSearchScope,
        librariesScope: GlobalSearchScope,
        configuration: CompilerConfiguration,
        moduleName: String = "TestModule",
        friendsPaths: List<Path> = emptyList(),
        getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ): FirSession {
        return FirSessionFactoryHelper.createSessionWithDependencies(
            Name.identifier(moduleName),
            JvmPlatforms.unspecifiedJvmPlatform,
            VfsBasedProjectEnvironment(
                project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                getPackagePartProvider
            ),
            configuration,
            PsiBasedProjectFileSearchScope(sourceScope),
            PsiBasedProjectFileSearchScope(librariesScope),
            incrementalCompilationContext = null,
            extensionRegistrars = emptyList(),
            needRegisterJavaElementFinder = true,
            dependenciesConfigurator = {
                friendDependencies(friendsPaths.map { it.pathString })
            }
        )
    }
}
