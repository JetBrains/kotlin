/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtCodeFragmentModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

public data class KtCodeFragmentModuleImpl(
    override val file: KtCodeFragment,
    override val contextModule: KtModule
) : KtCodeFragmentModule {
    override val project: Project
        get() = contextModule.project

    override val platform: TargetPlatform
        get() = contextModule.platform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = contextModule.analyzerServices

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)

    override val directRegularDependencies: List<KtModule>
        get() = listOf(contextModule) + contextModule.directRegularDependencies

    override val directDependsOnDependencies: List<KtModule>
        get() = contextModule.directDependsOnDependencies

    override val directFriendDependencies: List<KtModule>
        get() = contextModule.directFriendDependencies

    override val transitiveDependsOnDependencies: List<KtModule>
        get() = contextModule.transitiveDependsOnDependencies
}