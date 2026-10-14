/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.java

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityError
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

/**
 * Delegates directly to the compiler's [JavaModuleResolver] as we can use it in Standalone.
 */
@KaImplementationDetail
class KotlinStandaloneJavaModuleAccessibilityChecker(
    private val javaModuleResolver: JavaModuleResolver,
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
            is ModuleDoesNotReadUnnamedModule ->
                ModuleDoesNotReadUnnamedModule

            is ModuleDoesNotReadModule ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotReadModule(accessError.dependencyModuleName)

            is ModuleDoesNotExportPackage ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotExportPackage(accessError.dependencyModuleName)
        }
}
