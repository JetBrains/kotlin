/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMergeStrategy
import kotlin.reflect.KClass

internal class KotlinResolveExtensionGeneratedFileScopeMergeStrategy : KotlinGlobalSearchScopeMergeStrategy<KaBaseResolveExtensionGeneratedFilesScope> {
    override val targetType: KClass<KaBaseResolveExtensionGeneratedFilesScope> = KaBaseResolveExtensionGeneratedFilesScope::class

    override fun uniteScopes(scopes: List<KaBaseResolveExtensionGeneratedFilesScope>): List<GlobalSearchScope> {
        val useSiteModules =
            scopes.flatMap { scope ->
                scope.useSiteModules
            }
        return listOf(KaBaseResolveExtensionGeneratedFilesScope(useSiteModules))
    }
}