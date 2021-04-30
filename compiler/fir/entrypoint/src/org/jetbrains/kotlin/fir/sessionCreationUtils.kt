/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

@OptIn(PrivateForInline::class)
inline fun createSessionWithDependencies(
    name: Name,
    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    externalSessionProvider: FirProjectSessionProvider?,
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    lookupTracker: LookupTracker?,
    getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    getProviderAndScopeForIncrementalCompilation: () -> FirSessionFactory.ProviderAndScopeForIncrementalCompilation?,
    dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {},
    noinline sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit = {},
): FirSession {
    val dependencyList = DependencyListForCliModule.build(name, platform, analyzerServices, dependenciesConfigurator)
    return createSessionWithDependenciesImpl(
        name,
        dependencyList,
        externalSessionProvider,
        project,
        languageVersionSettings,
        sourceScope,
        librariesScope,
        lookupTracker,
        getPackagePartProvider,
        getProviderAndScopeForIncrementalCompilation,
        sessionConfigurator
    )
}

@OptIn(PrivateForInline::class)
inline fun createSessionWithDependencies(
    module: Module,
    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    externalSessionProvider: FirProjectSessionProvider?,
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    lookupTracker: LookupTracker?,
    getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    getProviderAndScopeForIncrementalCompilation: () -> FirSessionFactory.ProviderAndScopeForIncrementalCompilation?,
    noinline sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit = {},
): FirSession {
    val moduleName = Name.identifier(module.getModuleName())
    val dependencyList = DependencyListForCliModule.build(
        moduleName,
        platform,
        analyzerServices
    ) {
        friendDependencies(module.getFriendPaths())
        dependencies(module.getClasspathRoots())
    }
    return createSessionWithDependenciesImpl(
        moduleName,
        dependencyList,
        externalSessionProvider,
        project,
        languageVersionSettings,
        sourceScope,
        librariesScope,
        lookupTracker,
        getPackagePartProvider,
        getProviderAndScopeForIncrementalCompilation,
        sessionConfigurator
    )
}

@PrivateForInline
inline fun createSessionWithDependenciesImpl(
    moduleName: Name,
    dependencyListForCliModule: DependencyListForCliModule,
    externalSessionProvider: FirProjectSessionProvider?,
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    lookupTracker: LookupTracker?,
    getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    getProviderAndScopeForIncrementalCompilation: () -> FirSessionFactory.ProviderAndScopeForIncrementalCompilation?,
    noinline sessionConfigurator: FirSessionFactory.FirSessionConfigurator.() -> Unit,
): FirSession {
    val sessionProvider = externalSessionProvider ?: FirProjectSessionProvider()
    FirSessionFactory.createLibrarySession(
        moduleName,
        sessionProvider,
        dependencyListForCliModule.moduleDataProvider,
        librariesScope,
        project,
        getPackagePartProvider(librariesScope)
    )

    val mainModuleData = FirModuleDataImpl(
        moduleName,
        dependencyListForCliModule.regularDependencies,
        dependencyListForCliModule.dependsOnDependencies,
        dependencyListForCliModule.friendsDependencies,
        dependencyListForCliModule.platform,
        dependencyListForCliModule.analyzerServices
    )
    return FirSessionFactory.createJavaModuleBasedSession(
        mainModuleData,
        sessionProvider,
        sourceScope,
        project,
        providerAndScopeForIncrementalCompilation = getProviderAndScopeForIncrementalCompilation(),
        languageVersionSettings = languageVersionSettings,
        lookupTracker = lookupTracker,
        init = sessionConfigurator
    )
}
