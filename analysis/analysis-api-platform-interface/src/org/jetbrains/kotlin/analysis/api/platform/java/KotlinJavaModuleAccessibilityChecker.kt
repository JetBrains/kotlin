/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.java

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.FqName

/**
 * Allows checking whether one Java module has access to another Java module.
 */
public interface KotlinJavaModuleAccessibilityChecker {
    /**
     * Checks whether the Java module of [referencedFile] is accessible from the Java module of [useSiteFile]. If [referencedPackage] is
     * specified, additionally checks that the Java module of [referencedFile] exports the given package.
     *
     * @return `null` if the use-site module can access the referenced module (and the [referencedPackage] is exported), or an accessibility
     *  error otherwise.
     */
    public fun checkAccessibility(
        useSiteFile: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?,
    ): KotlinJavaModuleAccessibilityError?

    public companion object {
        public fun getInstance(project: Project): KotlinJavaModuleAccessibilityChecker = project.service()
    }
}

/**
 * An accessibility error returned by [KotlinJavaModuleAccessibilityChecker.checkAccessibility].
 */
public sealed class KotlinJavaModuleAccessibilityError {
    /**
     * The use-site module cannot read the referenced module because it is unnamed.
     */
    public object ModuleDoesNotReadUnnamedModule : KotlinJavaModuleAccessibilityError()

    /**
     * The use-site module cannot read the referenced module.
     */
    public data class ModuleDoesNotReadModule(val dependencyModuleName: String) : KotlinJavaModuleAccessibilityError()

    /**
     * The use-site module reads the referenced module, but the referenced package name is not exported by it.
     */
    public data class ModuleDoesNotExportPackage(val dependencyModuleName: String) : KotlinJavaModuleAccessibilityError()
}
