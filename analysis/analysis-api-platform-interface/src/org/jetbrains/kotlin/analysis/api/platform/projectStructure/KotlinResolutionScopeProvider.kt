/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public abstract class KotlinResolutionScopeProvider : KotlinPlatformComponent {
    public abstract fun getResolutionScope(module: KtModule): GlobalSearchScope

    public companion object {
        public fun getInstance(project: Project): KotlinResolutionScopeProvider =
            project.getService(KotlinResolutionScopeProvider::class.java)
    }
}
