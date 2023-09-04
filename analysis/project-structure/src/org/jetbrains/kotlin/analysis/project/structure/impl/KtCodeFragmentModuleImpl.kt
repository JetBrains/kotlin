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
import java.util.Objects

public class KtCodeFragmentModuleImpl(
    override val codeFragment: KtCodeFragment,
    override val contextModule: KtModule
) : KtCodeFragmentModule {
    override val project: Project
        get() = contextModule.project

    override val platform: TargetPlatform
        get() = contextModule.platform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = contextModule.analyzerServices

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(codeFragment)

    override val directRegularDependencies: List<KtModule>
        get() = contextModule.directRegularDependencies

    override val directDependsOnDependencies: List<KtModule>
        get() = contextModule.directDependsOnDependencies

    override val directFriendDependencies: List<KtModule>
        get() = listOf(contextModule) + contextModule.directFriendDependencies

    override val transitiveDependsOnDependencies: List<KtModule>
        get() = contextModule.transitiveDependsOnDependencies

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is KtCodeFragmentModuleImpl) {
            return codeFragment == other.codeFragment && contextModule == other.contextModule
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(codeFragment, contextModule)
    }
}