/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMergeStrategy
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinIntersectionScopeMergeTarget
import kotlin.reflect.KClass

/**
 * A [KotlinGlobalSearchScopeMergeStrategy] which merges intersection scopes in a union with *common (equal) individual scopes* that inherit
 * from [KotlinIntersectionScopeMergeTarget].
 *
 * This is legal due to the distributive property of sets. We can model scopes as sets of virtual files, so mathematical laws of sets also
 * apply to the intersections and unions of scopes.
 *
 * For example, take the following combination of sets `A`, `B`, and `C` (with `&` denoting intersection and `|` denoting union):
 *
 * ```
 * (B & A) | (C & A)
 * ```
 *
 * With the distributive law, we can factor out `A`:
 *
 * ```
 * (B | C) & A
 * ```
 *
 * Here, we've already reduced the scope construction from two intersections and a union to just a single intersection and union. But we can
 * go a step further and put `B | C` through the [KaGlobalSearchScopeMerger][org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaGlobalSearchScopeMerger]
 * again, which gives us the opportunity to merge `B` and `C`. This wouldn't have been possible without factoring out `A`.
 *
 * The content scopes of [KaLibraryModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule]s are a real-life case where
 * this is applied. The Analysis API restricts the content scope based on the library's target platform (see
 * `KaFirLibraryTargetPlatformContentScopeRefiner`), which is applied via an intersection. Because there is only a
 * single instance of the restriction scope per target platform, we can easily factor out the same restriction scope. This allows us to
 * merge the library scopes per target platform.
 */
internal class KotlinIntersectionScopeMergeStrategy : KotlinGlobalSearchScopeMergeStrategy<GlobalSearchScope> {
    /**
     * Because [com.intellij.psi.search.IntersectionScope] is package-private, the strategy has to target [GlobalSearchScope].
     */
    override val targetType: KClass<GlobalSearchScope> = GlobalSearchScope::class

    override fun uniteScopes(scopes: List<GlobalSearchScope>): List<GlobalSearchScope> {
        if (!scopes.any(GlobalSearchScopeUtil::isIntersectionScope)) return scopes

        val [intersectionScopes, restScopes] = scopes.partition(GlobalSearchScopeUtil::isIntersectionScope)
        return uniteIntersectionScopes(intersectionScopes) + restScopes
    }

    /**
     * Instead of trying to determine the scopes which should be factored out dynamically, we rely on [KotlinIntersectionScopeMergeTarget]
     * to mark the factors deterministically. In particular, using this strategy, we can inspect each intersection scope individually and
     * determine its bucket just based on its own set of merge targets. If we were to determine factors dynamically, we would have to
     * compare the components of the intersection scopes against each other, raising the algorithm's complexity.
     *
     * While this might not always result in the most optimal result, the strategy achieves near-optimal results for our particular use
     * cases. It also avoids the higher complexities of other possible algorithms, making the time to merge relatively predictable.
     */
    private fun uniteIntersectionScopes(scopes: List<GlobalSearchScope>): List<GlobalSearchScope> {
        if (scopes.size < 2) return scopes

        val project = scopes.firstNotNullOfOrNull { it.project } ?: return scopes

        val scopesByMergeTargets = mutableMapOf<Set<GlobalSearchScope>, MutableList<GlobalSearchScope>>()

        scopes.forEach { scope ->
            val componentScopes = GlobalSearchScopeUtil.flattenIntersectionScope(scope)

            // We put the non-factored scopes from each intersection scope into a bucket with its respective merge targets. For example,
            // if we have a union `(A & T1) | (B & T2) | (C & T1) | (D & T1 & T2)`, we have three buckets:
            //  - `T1` with `A` and `C`.
            //  - `T2` with `B`.
            //  - `T1 & T2` with `D`.
            //
            // Putting scopes into buckets like this is possible because the union operation is commutative and associative. So we can
            // essentially rewrite the union in the example to `((A & T1) | (C & T1)) | (B & T2) | (D & T1 & T2)`. Then we can process each
            // bucket like a separate union scope, as long as we reassemble all results into a union scope at the end.
            val [mergeTargets, remainingScopes] = componentScopes.partition { it is KotlinIntersectionScopeMergeTarget }

            // The remaining scopes need to be turned into an *intersection* again. For example, if we have `A & B & T1` and factor out
            // `T1`, we want the scope in the bucket to be `A & B`.
            val nonFactoredScope = intersectAll(remainingScopes, project)

            scopesByMergeTargets.computeIfAbsent(mergeTargets.toSet()) { mutableListOf() } += nonFactoredScope
        }

        val globalSearchScopeMerger = KaGlobalSearchScopeMerger.getInstance(project)

        val resultingScopes = scopesByMergeTargets.entries.flatMap { [mergeTargets, nonFactoredScopes] ->
            // If there are no merge targets, we have a situation where we are dealing with non-mergeable intersection scopes. For example,
            // we can have scopes `A1 & A2` and `C1 & C2` in a union `(A1 & A2) | (B & T1) | (C1 & C2)`. Here, `nonFactoredScopes` contains
            // `A1 & A2` and `C1 & C2`. Since the result is a list of scopes that should be turned into a union, we can return `A1 & A2` and
            // `C1 & C2` as-is.
            //
            // The short-circuit here is also important to avoid infinite recursion when calling `KaGlobalSearchScopeMerger` again. If we
            // don't have any merge targets, the input to the merge strategy is essentially the same as the `nonFactoredScopes` passed to
            // the recursive call of `KaGlobalSearchScopeMerger.union`. For example, when we merge `(A1 & A2) | (C1 & C2)` without merge
            // targets, we can fall into infinite recursion as such:
            //
            // intersection merge strategy: nonFactoredScopes = [(A1 & A2), (C1 & C2)]
            // --> globalSearchScopeMerger.union([(A1 & A2), (C1 & C2)])
            // --> intersection merge strategy: nonFactoredScopes = [(A1 & A2), (C1 & C2)]
            // --> globalSearchScopeMerger.union([(A1 & A2), (C1 & C2)])
            // --> ...
            if (mergeTargets.isEmpty()) return@flatMap nonFactoredScopes

            // This is the step where we recursively merge that `B | C` scope in `(B | C) & A`.
            val mergedNonFactoredScope = globalSearchScopeMerger.union(nonFactoredScopes)

            // Here, we reconstruct a single intersection scope from the merge targets and the non-factored scopes. For example, if we had
            // `(A & T1) | (C & T1)` initially, with the merge target `T1`, non-factored scopes `A` and `C`, and the merged non-factored
            // scope `A | C`, we ultimately reconstruct `T1 & (A | C)` here.
            listOf(intersectAll(mergeTargets.toList() + mergedNonFactoredScope, project))
        }

        // The results of each bucket merge need to be turned into a union scope, which matches up with the merge strategy needing to return
        // a list of scopes that will be turned into a union.
        return resultingScopes
    }

    private fun intersectAll(scopes: List<GlobalSearchScope>, project: Project): GlobalSearchScope {
        if (scopes.isEmpty()) return GlobalSearchScope.everythingScope(project)
        return scopes.reduce { acc, scope -> acc.intersectWith(scope) }
    }
}

