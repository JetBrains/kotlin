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
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import java.util.Objects

public class KtCodeFragmentModuleImpl(
    codeFragment: KtCodeFragment,
    override val contextModule: KtModule
) : KtCodeFragmentModule {
    private val codeFragmentRef = codeFragment.createSmartPointer()

    override val codeFragment: KtCodeFragment
        get() = codeFragmentRef.element?.takeIf { it.isValid } ?: error("Code fragment module is invalid")

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
            val selfCodeFragment = this.codeFragmentRef.element
            val otherCodeFragment = other.codeFragmentRef.element
            return selfCodeFragment != null && selfCodeFragment == otherCodeFragment && contextModule == other.contextModule
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(codeFragmentRef.element, contextModule)
    }
}