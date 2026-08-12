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
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.psiJavaInterop
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.nio.file.Path
import kotlin.io.path.pathString
import org.jetbrains.kotlin.jvm.environment.JvmClasspath

object FirTestSessionFactoryHelper {
    @ObsoleteTestInfrastructure
    fun createSessionForTests(
        projectEnvironment: VfsBasedProjectEnvironment,
        javaSourceScope: GlobalSearchScope,
        librariesClasspath: JvmClasspath = JvmClasspath.ProjectLibraries(),
        moduleName: String = "TestModule",
        friendsPaths: List<Path> = emptyList(),
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
    ): FirSession {
        val configuration = CompilerConfiguration.create().apply {
            this.languageVersionSettings = languageVersionSettings
        }
        return FirSessionFactoryHelper.createSessionWithDependencies(
            Name.identifier(moduleName),
            JvmPlatforms.unspecifiedJvmPlatform,
            projectEnvironment,
            configuration,
            projectEnvironment.psiJavaInterop(javaSources = { javaSourceScope }),
            librariesClasspath,
            incrementalCompilationContext = null,
            extensionRegistrars = emptyList(),
            dependenciesConfigurator = {
                friendDependencies(friendsPaths.map { it.pathString })
            }
        )
    }

    @ObsoleteTestInfrastructure
    fun createSessionForTests(
        project: Project,
        sourceScope: GlobalSearchScope,
        configuration: CompilerConfiguration,
        moduleName: String = "TestModule",
        friendsPaths: List<Path> = emptyList(),
        getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ): FirSession {
        val projectEnvironment = VfsBasedProjectEnvironment(
            project,
            VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            getPackagePartProvider
        )
        return FirSessionFactoryHelper.createSessionWithDependencies(
            Name.identifier(moduleName),
            JvmPlatforms.unspecifiedJvmPlatform,
            projectEnvironment,
            configuration,
            projectEnvironment.psiJavaInterop(javaSources = { sourceScope }),
            JvmClasspath.ProjectLibraries(),
            incrementalCompilationContext = null,
            extensionRegistrars = emptyList(),
            dependenciesConfigurator = {
                friendDependencies(friendsPaths.map { it.pathString })
            }
        )
    }
}
