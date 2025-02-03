/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public interface KaContentScopeProvider : KaEngineService {
    /**
     * Calculates a [KaModule.contentScope] from [KaModule.baseContentScope] for the given [KaModule] using [KotlinContentScopeRefiner]s.
     */
    public fun getRefinedContentScope(module: KaModule): GlobalSearchScope

    public companion object {
        public fun getInstance(project: Project): KaContentScopeProvider = project.service()
    }
}