/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

internal class KtNotUnderContentRootModuleImpl(
    override val directRegularDependencies: List<KtModule> = emptyList(),
    override val directRefinementDependencies: List<KtModule> = emptyList(),
    override val directFriendDependencies: List<KtModule> = emptyList(),
    override val platform: TargetPlatform = JvmPlatforms.defaultJvmPlatform,
    psiFile: PsiFile? = null,
    override val moduleDescription: String,
) : KtNotUnderContentRootModule, KtModuleWithPlatform {
    override val analyzerServices: PlatformDependentAnalyzerServices = super.analyzerServices

    override val contentScope: GlobalSearchScope =
        if (psiFile != null) GlobalSearchScope.fileScope(psiFile) else GlobalSearchScope.EMPTY_SCOPE
}