/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public interface KotlinResolutionScopeProvider : KotlinPlatformComponent {
    /**
     * Returns a [GlobalSearchScope] which covers the resolvable content of [module].
     */
    public fun getResolutionScope(module: KaModule): GlobalSearchScope

    public companion object {
        public fun getInstance(project: Project): KotlinResolutionScopeProvider = project.service()
    }
}
