/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * A contributor to the resolution scope provided by [KotlinResolutionScopeProvider].
 *
 * This allows extensions providing synthetic non-Kotlin source to bring those source files
 * into the relevant module, library, or SDK scopes in a backend-agnostic manner. For extensions
 * producing Kotlin source, please see the `KaResolveExtensionProvider` API.
 */
public interface KotlinResolutionScopeEnlarger {

    public fun getAdditionalResolutionScope(module: KaModule): GlobalSearchScope?

    public companion object {
        public val EP_NAME: ExtensionPointName<KotlinResolutionScopeEnlarger> =
            ExtensionPointName<KotlinResolutionScopeEnlarger>(
                "org.jetbrains.kotlin.kotlinResolutionScopeEnlarger"
            )

        public fun getEnlargedResolutionScope(
            module: KaModule,
            project: Project = module.project,
        ): GlobalSearchScope {
            val baseScope =
                KotlinResolutionScopeProvider.getInstance(project).getResolutionScope(module)
            val additionalScopes =
                EP_NAME.getExtensionList(project).mapNotNull {
                    it.getAdditionalResolutionScope(module)?.takeIf { scope ->
                        !GlobalSearchScope.isEmptyScope(scope)
                    }
                }

            return if (additionalScopes.isNotEmpty()) {
                KotlinGlobalSearchScopeMerger.getInstance(project)
                    .union(listOf(baseScope) + additionalScopes)
            } else {
                baseScope
            }
        }
    }
}
