/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent


/**
 * [KotlinGlobalSearchScopeMergerStrategyProvider] is used to provide various strategies
 * to merge a list of [GlobalSearchScope]s into a single [GlobalSearchScope].
 * Is used by implementations of [KotlinGlobalSearchScopeMerger] to optimize the merging process and flatten the resulting scope.
 */
public interface KotlinGlobalSearchScopeMergerStrategyProvider : KotlinPlatformComponent {
    /**
     * Defines whether this [KotlinGlobalSearchScopeMergerStrategyProvider] can be applied to [scope].
     * Note that [KotlinGlobalSearchScopeMerger] applies the narrowest strategies first.
     * It means that, given a list of [GlobalSearchScope]s,
     * [KotlinGlobalSearchScopeMerger] will start with a strategy that [canMerge] the least number of elements.
     */
    public fun canMerge(scope: GlobalSearchScope): Boolean

    /**
     * Merges a list of [GlobalSearchScope]s into a single [GlobalSearchScope].
     * All the usages of [mergeScopes] must follow two constraints:
     * - [scopes] must only contain elements, for which [canMerge] returns 'true'.
     * - [scopes] must be non-empty.
     */
    public fun mergeScopes(scopes: List<GlobalSearchScope>): GlobalSearchScope

    public companion object {
        public val EP_NAME: ExtensionPointName<KotlinGlobalSearchScopeMergerStrategyProvider> =
            ExtensionPointName<KotlinGlobalSearchScopeMergerStrategyProvider>(
                "org.jetbrains.kotlin.kotlinGlobalSearchScopeMergerStrategyProvider"
            )

        public fun getMergeStrategies(
            project: Project,
        ): List<KotlinGlobalSearchScopeMergerStrategyProvider> = EP_NAME.getExtensionList(project)
    }
}

