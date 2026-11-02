/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMergeStrategy
import kotlin.reflect.KClass

internal class KotlinResolveExtensionGeneratedFileScopeMergeStrategy : KotlinGlobalSearchScopeMergeStrategy<KaFirResolveExtensionGeneratedFilesScope> {
    override val targetType: KClass<KaFirResolveExtensionGeneratedFilesScope> = KaFirResolveExtensionGeneratedFilesScope::class

    override fun uniteScopes(scopes: List<KaFirResolveExtensionGeneratedFilesScope>, project: Project): List<GlobalSearchScope> {
        val useSiteModules =
            scopes.flatMap { scope ->
                scope.useSiteModules
            }
        return listOf(KaFirResolveExtensionGeneratedFilesScope(useSiteModules))
    }
}
