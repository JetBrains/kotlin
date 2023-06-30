/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule

/**
 * Provide module which contains dependencies of the library.
 *
 * Is required for navigation in IJ monorepo from kotlin compiler library sources to IJ platform sources.
 * Kotlin compiler depends on platform but this dependency is not represented as jars in monorepo
 * but should be replaced with given sources.
 */
public interface KotlinAnchorModuleProvider {
    public fun getAnchorModule(libraryModule: KtLibraryModule): KtSourceModule?

    public companion object {
        public fun getInstance(project: Project): KotlinAnchorModuleProvider? =
            project.getService(KotlinAnchorModuleProvider::class.java)
    }
}