/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Provides `actual` declarations for the given `expect` [KtDeclaration].
 *
 * The implementation should be consistent with the `KaSymbolRelationProvider.getExpectsForActual`.
 */
public interface KotlinActualDeclarationProvider : KotlinOptionalPlatformComponent {
    /**
     * Returns `actual` declarations for the given `expect` [KtDeclaration] from all available implementing modules.
     */
    public fun getActualDeclarations(declaration: KtDeclaration): Sequence<KtDeclaration>

    public companion object {
        public fun getInstance(project: Project): KotlinActualDeclarationProvider? = project.serviceOrNull()
    }
}