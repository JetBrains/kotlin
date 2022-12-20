/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.project.structure.computeTransitiveDependsOnDependencies
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

internal class KtNotUnderContentRootModuleImpl(
    override val name: String,
    override val directRegularDependencies: List<KtModule> = emptyList(),
    override val directDependsOnDependencies: List<KtModule> = emptyList(),
    override val directFriendDependencies: List<KtModule> = emptyList(),
    override val platform: TargetPlatform = JvmPlatforms.defaultJvmPlatform,
    override val file: PsiFile? = null,
    override val moduleDescription: String,
    override val project: Project,
) : KtNotUnderContentRootModule, KtModuleWithPlatform {
    override val transitiveDependsOnDependencies: List<KtModule> by lazy { computeTransitiveDependsOnDependencies(directDependsOnDependencies) }
    override val analyzerServices: PlatformDependentAnalyzerServices = super.analyzerServices

    override val contentScope: GlobalSearchScope =
        if (file != null) GlobalSearchScope.fileScope(file) else GlobalSearchScope.EMPTY_SCOPE
}
