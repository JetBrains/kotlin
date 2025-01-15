/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * A contributor to [KaModule.contentScope].
 *
 * This extension point allows extending and restricting content scopes inside [KaModule].
 * All the refiners are lazily applied to [KaModule.baseContentScope] to build [KaModule.contentScope].
 * The computation of the refined content scope happens inside [KaContentScopeProvider].
 *
 * This allows extensions providing synthetic non-Kotlin sources to bring those source files
 * into the relevant module, library, or SDK scopes and shadow existing sources in a backend-agnostic manner.
 *
 * Note that implementations of [KotlinContentScopeRefiner] cannot access [KaModule.contentScope],
 * as it's not yet built when [getEnlargementScopes]/[getRestrictionScopes] are called.
 *
 * For extensions producing Kotlin source, please see the [org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider].
 */
public interface KotlinContentScopeRefiner : KotlinPlatformComponent {
    /**
     * Given a [KaModule], [getEnlargementScopes] returns [GlobalSearchScope]s,
     * which should enlarge [KaModule.baseContentScope] to form [KaModule.contentScope].
     *
     * If some file from [getEnlargementScopes] is already contained in [KaModule.baseContentScope],
     * the resulting [KaModule.contentScope] will function the same way w.r.t. this file,
     * i.e., as if this file wasn't initially present.
     */
    public fun getEnlargementScopes(module: KaModule): List<GlobalSearchScope> = emptyList()

    /**
     * Given a [KaModule], [getRestrictionScopes] returns [GlobalSearchScope]s,
     * which should restrict [KaModule.baseContentScope] to form [KaModule.contentScope].
     *
     * If some file from [getRestrictionScopes] is not contained in [KaModule.baseContentScope],
     * the resulting [KaModule.contentScope] will function the same way w.r.t. this file,
     * i.e., as if this file was initially present.
     */
    public fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> = emptyList()

    public companion object {
        public val EP_NAME: ExtensionPointName<KotlinContentScopeRefiner> =
            ExtensionPointName<KotlinContentScopeRefiner>(
                "org.jetbrains.kotlin.kotlinContentScopeRefiner"
            )

        public fun getRefiners(
            project: Project,
        ): List<KotlinContentScopeRefiner> = EP_NAME.getExtensionList(project)
    }
}
