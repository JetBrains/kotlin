/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * Merges [GlobalSearchScope]s according to registered [KotlinGlobalSearchScopeMergeStrategy] with the goal
 * of creating an optimized and flattened combined scope.
 */
@KaPlatformInterface
public interface KaGlobalSearchScopeMerger : KaEngineService {
    /**
     * Creates a merged [GlobalSearchScope] which represents a *union* of all [scopes].
     */
    public fun union(scopes: Collection<GlobalSearchScope>): GlobalSearchScope

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KaGlobalSearchScopeMerger = project.service()
    }
}
