/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.java

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityError
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

/**
 * Delegates directly to [CliJavaModuleResolver] as we can use it in Standalone.
 */
internal class KotlinStandaloneJavaModuleAccessibilityChecker(
    private val javaModuleResolver: CliJavaModuleResolver,
) : KotlinJavaModuleAccessibilityChecker {
    override fun checkAccessibility(
        useSiteFile: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?,
    ): KotlinJavaModuleAccessibilityError? {
        val accessError = javaModuleResolver.checkAccessibility(useSiteFile, referencedFile, referencedPackage)
        return accessError?.let(::convertAccessError)
    }

    private fun convertAccessError(accessError: JavaModuleResolver.AccessError): KotlinJavaModuleAccessibilityError =
        when (accessError) {
            is JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotReadUnnamedModule

            is JavaModuleResolver.AccessError.ModuleDoesNotReadModule ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotReadModule(accessError.dependencyModuleName)

            is JavaModuleResolver.AccessError.ModuleDoesNotExportPackage ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotExportPackage(accessError.dependencyModuleName)
        }
}
