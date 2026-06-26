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
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.resolve.jvm.CommonJavaClassFinder
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.TopPackageNamesProvider

fun Project.createJavaClassFinder(
    scope: GlobalSearchScope,
    annotationProvider: JavaAnnotationProvider?
): JavaClassFinder {
    return JavaClassFinderImpl(annotationProvider, this@createJavaClassFinder, scope)
}

class JavaClassFinderImpl(
    override val annotationProvider: JavaAnnotationProvider?,
    project: Project,
    javaScope: GlobalSearchScope,
) : CommonJavaClassFinder() {
    override val javaFacade: KotlinJavaPsiFacade = KotlinJavaPsiFacade.getInstance(project)
    override val javaSearchScope: GlobalSearchScope = if (javaScope == GlobalSearchScope.EMPTY_SCOPE) {
        GlobalSearchScope.EMPTY_SCOPE
    } else {
        FilterOutKotlinSourceFilesScope(project, javaScope)
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
