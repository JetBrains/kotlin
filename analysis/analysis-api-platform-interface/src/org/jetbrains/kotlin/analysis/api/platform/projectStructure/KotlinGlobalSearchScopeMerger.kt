/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent

/**
 * Merges [GlobalSearchScope]s according to platform-specific strategies with the goal of creating an optimized combined scope. If possible,
 * the merger should especially try to merge scopes which can be the basis of [KaModule.contentScope][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule.contentScope]s.
 *
 * If there are no good scope merging strategies, [KotlinSimpleGlobalSearchScopeMerger] should be registered by the platform.
 */
public interface KotlinGlobalSearchScopeMerger : KotlinPlatformComponent {
    /**
     * Creates a merged [GlobalSearchScope] which represents a *union* of all [scopes].
     */
    public fun union(scopes: Collection<GlobalSearchScope>): GlobalSearchScope

    public companion object {
        public fun getInstance(project: Project): KotlinGlobalSearchScopeMerger = project.service()
    }
}
