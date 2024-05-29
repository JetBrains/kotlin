/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule

/**
 * [KotlinAnchorModuleProvider] provides modules which contain dependencies of libraries.
 *
 * In the IJ monorepo, anchor modules are required for navigation from Kotlin compiler library sources to IJ platform sources. The Kotlin
 * compiler depends on the IJ platform, but this dependency is not represented as JARs in the monorepo, but rather by certain monorepo
 * source modules, which are made visible to the Kotlin compiler library sources as dependencies via an anchor module.
 */
public interface KotlinAnchorModuleProvider {
    public fun getAnchorModule(libraryModule: KtLibraryModule): KtSourceModule?

    /**
     * Returns all anchor modules configured in the project.
     */
    public fun getAllAnchorModules(): Collection<KtSourceModule>

    public companion object {
        public fun getInstance(project: Project): KotlinAnchorModuleProvider? =
            project.getService(KotlinAnchorModuleProvider::class.java)
    }
}
