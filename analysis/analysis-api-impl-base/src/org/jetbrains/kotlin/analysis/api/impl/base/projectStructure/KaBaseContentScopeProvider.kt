/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaContentScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

internal class KaBaseContentScopeProvider : KaContentScopeProvider {
    @OptIn(KaPlatformInterface::class)
    override fun getRefinedContentScope(module: KaModule): GlobalSearchScope {
        val baseContentScope = module.contentScope

        val refiners = KotlinContentScopeRefiner.getRefiners(module.project).ifEmpty {
            return baseContentScope
        }

        val enlargementScopes = mutableListOf<GlobalSearchScope>(baseContentScope)
        val shadowedScopes = mutableListOf<GlobalSearchScope>()

        refiners.forEach { refiner ->
            enlargementScopes.addAll(
                refiner.getEnlargementScopes(module).filter { !GlobalSearchScope.isEmptyScope(it) }
            )

            shadowedScopes.addAll(
                refiner.getRestrictionScopes(module).filter { !GlobalSearchScope.isEmptyScope(it) }
            )
        }

        val scopeMerger = KotlinGlobalSearchScopeMerger.getInstance(module.project)

        val mergedEnlargementScope = scopeMerger.union(enlargementScopes)
        if (shadowedScopes.isEmpty()) {
            return mergedEnlargementScope
        }

        val mergedShadowedScope = scopeMerger.union(shadowedScopes)

        return mergedEnlargementScope.intersectWith(GlobalSearchScope.notScope(mergedShadowedScope))
    }
}