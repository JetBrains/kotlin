/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

/**
 * A [KotlinGlobalSearchScopeMerger] which uses registered [KotlinGlobalSearchScopeMergerStrategyProvider]s
 * to optimize [com.intellij.psi.search.GlobalSearchScope] merging process and flatten the resulting scope.
 */
public class KotlinOptimizingGlobalSearchScopeMerger : KotlinGlobalSearchScopeMerger {
    override fun union(project: Project, scopes: Collection<GlobalSearchScope>): GlobalSearchScope {
        if (scopes.isEmpty()) {
            return GlobalSearchScope.EMPTY_SCOPE
        }

        val providedStrategies =
            KotlinGlobalSearchScopeMergerStrategyProvider.Companion.getMergeStrategies(project)

        var currentScopesSet = scopes
        var narrowestApplicableStrategy = narrowestStrategyIfShouldProceed(providedStrategies, currentScopesSet)

        while (narrowestApplicableStrategy != null) {
            val (combinableScopes, otherScopes) = currentScopesSet.partition { narrowestApplicableStrategy.canMerge(it) }
            currentScopesSet = listOf(narrowestApplicableStrategy.mergeScopes(combinableScopes)) + otherScopes
            narrowestApplicableStrategy = narrowestStrategyIfShouldProceed(providedStrategies, currentScopesSet)
        }

        return GlobalSearchScope.union(currentScopesSet)
    }

    private fun narrowestStrategyIfShouldProceed(
        strategies: List<KotlinGlobalSearchScopeMergerStrategyProvider>,
        scopes: Collection<GlobalSearchScope>
    ): KotlinGlobalSearchScopeMergerStrategyProvider? {
        val narrowestApplicableStrategy =
            strategies.mapNotNull { strategy ->
                val applicability = scopes.count { strategy.canMerge(it) }
                (strategy to applicability).takeIf { applicability >= 2 }
            }.minByOrNull { (_, applicability) -> applicability }?.first

        return narrowestApplicableStrategy
    }
}