/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule

/**
 * [KotlinModuleOutputProvider] provides build output directories for [KaModule]s.
 */
public interface KotlinModuleOutputProvider : KotlinOptionalPlatformComponent {
    /**
     * Returns the compilation output file/directory for the [module], or `null` if it is unset or unknown.
     */
    public fun getCompilationOutput(module: KaSourceModule): VirtualFile?

    public companion object {
        public fun getInstance(project: Project): KotlinModuleOutputProvider = project.service()
    }
}