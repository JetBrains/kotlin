/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.computeTransitiveDependsOnDependencies
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

@OptIn(KaExperimentalApi::class)
internal class KaNotUnderContentRootModuleImpl(
    override val name: String,
    override val directRegularDependencies: List<KaModule> = emptyList(),
    override val directDependsOnDependencies: List<KaModule> = emptyList(),
    override val directFriendDependencies: List<KaModule> = emptyList(),
    override val targetPlatform: TargetPlatform = JvmPlatforms.defaultJvmPlatform,
    override val file: PsiFile? = null,
    override val moduleDescription: String,
    override val project: Project,
) : KaNotUnderContentRootModule, KtModuleWithPlatform {
    override val transitiveDependsOnDependencies: List<KaModule> by lazy {
        computeTransitiveDependsOnDependencies(directDependsOnDependencies)
    }

    override val contentScope: GlobalSearchScope =
        if (file != null) GlobalSearchScope.fileScope(file) else GlobalSearchScope.EMPTY_SCOPE
}
