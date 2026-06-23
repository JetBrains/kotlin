/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.TopPackageNamesProvider

fun Project.createJavaClassFinder(
    scope: GlobalSearchScope,
    annotationProvider: JavaAnnotationProvider?
): JavaClassFinder {
    return JavaClassFinderImpl(annotationProvider, this@createJavaClassFinder, scope)
}

class JavaClassFinderImpl(
    private val annotationProvider: JavaAnnotationProvider?,
    project: Project,
    javaScope: GlobalSearchScope,
) : JavaClassFinder {
    private val javaFacade: KotlinJavaPsiFacade = KotlinJavaPsiFacade.getInstance(project)
    private val javaSearchScope: GlobalSearchScope = if (javaScope == GlobalSearchScope.EMPTY_SCOPE) {
        GlobalSearchScope.EMPTY_SCOPE
    } else {
        FilterOutKotlinSourceFilesScope(project, javaScope)
    }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        return javaFacade.findClass(request, javaSearchScope)
    }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> {
        return javaFacade.findClasses(request, javaSearchScope)
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        return javaFacade.findPackage(fqName.asString(), javaSearchScope)
            ?.let { createJavaPackage(it, mayHaveAnnotations) }
    }

    private fun createJavaPackage(
        psiPackage: PsiPackage,
        mayHaveAnnotations: Boolean,
    ): JavaPackageImpl {
        val project = javaFacade.project
        val sourceFactory = JavaElementSourceFactory.getInstance(project)
        return JavaPackageImpl(
            psiPackageSource = sourceFactory.createPsiSource(psiPackage),
            scope = javaSearchScope,
            mayHaveAnnotations = mayHaveAnnotations,
            annotationsProvider = annotationProvider
        )
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? {
        return javaFacade.knownClassNamesInPackage(packageFqName, javaSearchScope)
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean {
        return javaFacade.canComputeKnownClassNamesInPackage()
    }

    private class FilterOutKotlinSourceFilesScope(
        private val myProject: Project,
        baseScope: GlobalSearchScope,
    ) : DelegatingGlobalSearchScope(baseScope), TopPackageNamesProvider {
        override val topPackageNames: Set<String>?
            get() = (myBaseScope as? TopPackageNamesProvider)?.topPackageNames

        override fun contains(file: VirtualFile): Boolean {
            // KTIJ-20095: optimization to avoid heavy file.fileType calculation
            val extension = file.extension
            val ktFile =
                when {
                    file.isDirectory -> false
                    extension == KotlinFileType.EXTENSION -> true
                    extension == JavaFileType.DEFAULT_EXTENSION || extension == JavaClassFileType.INSTANCE.defaultExtension -> false
                    else -> {
                        val fileTypeByFileName = FileTypeRegistry.getInstance().getFileTypeByFileName(file.name)
                        fileTypeByFileName == KotlinFileType.INSTANCE || fileTypeByFileName == UnknownFileType.INSTANCE &&
                                FileTypeRegistry.getInstance().isFileOfType(file, KotlinFileType.INSTANCE)
                    }
                }
            return !ktFile && myBaseScope.contains(file)
        }

        @Suppress("NonExtendableApiUsage")
        override fun getProject(): Project {
            return myProject
        }

        override fun toString(): String = "JCFI: $myBaseScope"

    }
}
