/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import kotlin.reflect.KClass

/**
 * [KotlinGlobalSearchScopeMergeStrategy] is used to declare a strategy for
 * merging a list of [GlobalSearchScope]s into a single [GlobalSearchScope].
 * Is used by implementations of [KaGlobalSearchScopeMerger] to optimize the merging process and flatten the resulting scope.
 *
 * Note that [KotlinGlobalSearchScopeMergeStrategy] are applied in the order they are registered.
 */
@KaPlatformInterface
@KaExperimentalApi
public interface KotlinGlobalSearchScopeMergeStrategy<T : Any> : KotlinPlatformComponent {
    /**
     * Defines the target type [KotlinGlobalSearchScopeMergeStrategy] works with.
     * All scopes passed to [uniteScopes] are guaranteed to be a subtype of [targetType].
     */
    public val targetType: KClass<T>

    /**
     * Merges a list of [GlobalSearchScope]s into an optimized set of [GlobalSearchScope]s.
     * - If [scopes] cannot be optimized, [uniteScopes] should return the same [scopes] list.
     * - If [scopes] can be merged into one [GlobalSearchScope.EMPTY_SCOPE], then [uniteScopes] should return an empty list.
     */
    public fun uniteScopes(scopes: List<T>): List<GlobalSearchScope>

    @KaPlatformInterface
    public companion object {
        public val EP_NAME: ExtensionPointName<KotlinGlobalSearchScopeMergeStrategy<*>> =
            ExtensionPointName<KotlinGlobalSearchScopeMergeStrategy<*>>(
                "org.jetbrains.kotlin.kotlinGlobalSearchScopeMergeStrategy"
            )

        public fun getMergeStrategies(
            project: Project,
        ): List<KotlinGlobalSearchScopeMergeStrategy<*>> = EP_NAME.getExtensionList(project)
    }
}

/**
 * A marker interface for [GlobalSearchScope]s that can be factored out of an intersection scope using the distributive property. This helps
 * the intersection scope merge strategy to pick the correct targets.
 *
 * The marker should be applied to scopes which have the following characteristics:
 *
 * 1. They are frequently applied in intersection scopes (e.g., via [KotlinContentScopeRefiner.getRestrictionScopes]).
 * 2. The same scope is applied in multiple intersection scopes, allowing the scope merger to merge intersections by factoring it out.
 *
 * ### Illustration
 *
 * Take the following combination of sets `A`, `B`, and `C` (with `&` denoting intersection and `|` denoting union):
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
 * Exactly the scope `A` would be marked with [KotlinIntersectionScopeMergeTarget].
 */
@KaPlatformInterface
@KaExperimentalApi
public interface KotlinIntersectionScopeMergeTarget
