/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform

class KaLibraryFallbackDependenciesModuleImpl(
    override val dependentLibrary: KaLibraryModule,
) : KaModuleBase(), KaLibraryFallbackDependenciesModule {
    override val directRegularDependencies: List<KaModule> get() = emptyList()
    override val directDependsOnDependencies: List<KaModule> get() = emptyList()
    override val directFriendDependencies: List<KaModule> get() = emptyList()

    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope
        get() = ProjectScope.getLibrariesScope(project).intersectWith(GlobalSearchScope.notScope(dependentLibrary.contentScope))

    override val targetPlatform: TargetPlatform
        get() = dependentLibrary.targetPlatform

    override val project: Project
        get() = dependentLibrary.project

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Fallback dependencies module for '${dependentLibrary.moduleDescription}'"
}
