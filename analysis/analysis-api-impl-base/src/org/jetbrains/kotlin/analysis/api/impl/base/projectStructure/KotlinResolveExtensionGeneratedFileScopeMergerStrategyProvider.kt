/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolveExtensionGeneratedFilesScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMergerStrategyProvider

class KotlinResolveExtensionGeneratedFileScopeMergerStrategyProvider : KotlinGlobalSearchScopeMergerStrategyProvider {
    override fun canMerge(scope: GlobalSearchScope): Boolean = scope is KaResolveExtensionGeneratedFilesScope

    override fun mergeScopes(scopes: List<GlobalSearchScope>): GlobalSearchScope {
        val useSiteModules =
            scopes.filterIsInstance<KaBaseResolveExtensionGeneratedFilesScope>().flatMap { it.useSiteModules }
        return KaBaseResolveExtensionGeneratedFilesScope(useSiteModules)
    }
}