/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJvmModuleInfo
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.Name

fun createSessionWithDependencies(
    name: Name,
    friendPaths: List<String>,
    outputDirectory: String?,
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    lookupTracker: LookupTracker?,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit = {}
): FirSession {
    return createSessionWithDependencies(
        name.identifier,
        project,
        languageVersionSettings,
        sourceScope,
        librariesScope,
        lookupTracker,
        packagePartProvider,
        sessionConfigurator
    ) {
        FirJvmModuleInfo(name, it, friendPaths, outputDirectory)
    }
}

fun createSessionWithDependencies(
    module: Module,
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    lookupTracker: LookupTracker?,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit = {}
): FirSession {
    return createSessionWithDependencies(
        module.getModuleName(),
        project,
        languageVersionSettings,
        sourceScope,
        librariesScope,
        lookupTracker,
        packagePartProvider,
        sessionConfigurator
    ) {
        FirJvmModuleInfo(module, it)
    }
}

private inline fun createSessionWithDependencies(
    moduleName: String,
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    lookupTracker: LookupTracker?,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    noinline sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit,
    moduleInfoProvider: (dependencies: List<ModuleInfo>) -> ModuleInfo,
): FirSession {
    val provider = FirProjectSessionProvider()
    val librariesModuleInfo = FirJvmModuleInfo.createForLibraries(moduleName)
    FirSessionFactory.createLibrarySession(
        librariesModuleInfo, provider, librariesScope,
        project, packagePartProvider(librariesScope)
    )
    return FirSessionFactory.createJavaModuleBasedSession(
        moduleInfoProvider(listOf(librariesModuleInfo)),
        provider,
        sourceScope,
        project,
        languageVersionSettings = languageVersionSettings,
        lookupTracker = lookupTracker,
        init = sessionConfigurator
    )
}
