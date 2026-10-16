/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAnnotationsProvider
import org.jetbrains.kotlin.analysis.api.platform.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityError
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleJavaAnnotationsProvider
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModulePsiAnnotationsProvider
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl
import org.jetbrains.kotlin.load.java.structure.impl.convert
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

internal class KaBaseJavaModuleResolver(private val project: Project) : JavaModuleResolver {
    private val annotationCache = NullableCaffeineCache<ClassId, List<JavaAnnotation>> { it.maximumSize(1000) }

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        return annotationCache.getOrPut(classId) {
            val provider = KotlinJavaModuleAnnotationsProvider.getInstance(project)

            when (provider) {
                is KotlinJavaModulePsiAnnotationsProvider -> {
                    provider.getAnnotationsForModuleOwnerOfClass(classId)?.convert { psiAnnotation ->
                        val sourceFactory = JavaElementSourceFactory.getInstance(project)
                        val psiJavaSource = sourceFactory.createPsiSource(psiAnnotation)
                        JavaAnnotationImpl(psiJavaSource)
                    }
                }
                is KotlinJavaModuleJavaAnnotationsProvider -> {
                    provider.getAnnotationsForModuleOwnerOfClass(classId)
                }
            }
        }
    }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? =
        KotlinJavaModuleAccessibilityChecker.getInstance(project)
            .checkAccessibility(fileFromOurModule, referencedFile, referencedPackage)
            ?.let(::convertAccessibilityError)

    private fun convertAccessibilityError(accessibilityError: KotlinJavaModuleAccessibilityError): JavaModuleResolver.AccessError =
        when (accessibilityError) {
            is KotlinJavaModuleAccessibilityError.ModuleDoesNotReadUnnamedModule ->
                JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule

            is KotlinJavaModuleAccessibilityError.ModuleDoesNotReadModule ->
                JavaModuleResolver.AccessError.ModuleDoesNotReadModule(accessibilityError.dependencyModuleName)

            is KotlinJavaModuleAccessibilityError.ModuleDoesNotExportPackage ->
                JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(accessibilityError.dependencyModuleName)
        }
}
