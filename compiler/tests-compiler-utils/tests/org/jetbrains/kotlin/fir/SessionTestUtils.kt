/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name

fun createSessionForTests(
    environment: KotlinCoreEnvironment,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope = GlobalSearchScope.notScope(sourceScope),
    moduleName: String = "TestModule",
    friendPaths: List<String> = emptyList(),
    lookupTracker: LookupTracker? = null
): FirSession = createSessionForTests(
    environment.project,
    sourceScope,
    librariesScope,
    moduleName,
    friendPaths,
    lookupTracker,
    environment::createPackagePartProvider
)

fun createSessionForTests(
    project: Project,
    sourceScope: GlobalSearchScope,
    librariesScope: GlobalSearchScope,
    moduleName: String = "TestModule",
    friendPaths: List<String> = emptyList(),
    lookupTracker: LookupTracker? = null,
    getPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    getAdditionalModulePackagePartProvider: (GlobalSearchScope) -> PackagePartProvider? = { null }
): FirSession {
    return createSessionWithDependencies(
        Name.identifier(moduleName),
        friendPaths,
        outputDirectory = null,
        project,
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        sourceScope,
        librariesScope,
        lookupTracker,
        getPackagePartProvider,
        getAdditionalModulePackagePartProvider
    )
}

