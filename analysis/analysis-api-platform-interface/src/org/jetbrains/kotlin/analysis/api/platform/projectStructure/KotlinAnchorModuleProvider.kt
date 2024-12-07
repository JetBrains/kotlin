/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule

/**
 * [KotlinAnchorModuleProvider] provides modules which contain dependencies of libraries.
 *
 * In the IJ monorepo, anchor modules are required for navigation from Kotlin compiler library sources to IJ platform sources. The Kotlin
 * compiler depends on the IJ platform, but this dependency is not represented as JARs in the monorepo, but rather by certain monorepo
 * source modules, which are made visible to the Kotlin compiler library sources as dependencies via an anchor module.
 */
public interface KotlinAnchorModuleProvider : KotlinOptionalPlatformComponent {
    public fun getAnchorModule(libraryModule: KaLibraryModule): KaSourceModule?

    /**
     * Returns all anchor modules configured in the project.
     */
    public fun getAllAnchorModules(): Collection<KaSourceModule>

    /**
     * Returns all anchor modules configured in the project if they have already been computed and are not invalidated.
     * This function must only be called from a write action to ensure that they are not being computed from another read action.
     *
     * @return The anchor modules if they are computed and the result is the same as [getAllAnchorModules], or `null` otherwise.
     */
    public fun getAllAnchorModulesIfComputed(): Collection<KaSourceModule>?

    public companion object {
        public fun getInstance(project: Project): KotlinAnchorModuleProvider? = project.serviceOrNull()
    }
}
