/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.java

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileClassProvider

/**
 * Provides Java classes for decompiled Kotlin class files.
 *
 * Kotlin class files are decompiled into [KtClsFile]s instead of Java class files, so the Java class finder, which looks up classes in a
 * class file via [KtFile.getClasses], relies on this provider to see Kotlin library classes such as `kotlin.collections.AbstractList`.
 *
 * The provided classes are plain Java classes built from the class file, not light classes: the same Java class finder is used by the
 * compiler's Java resolution, which must not be given light classes.
 *
 * Classes for source files are not provided here, as their light classes are found by
 * [JavaElementFinder][org.jetbrains.kotlin.asJava.finder.JavaElementFinder].
 */
internal class KotlinStandaloneFileClassProvider(private val project: Project) : KtFileClassProvider {
    override fun getFileClasses(file: KtFile): Array<PsiClass> {
        if (file !is KtClsFile) return PsiClass.EMPTY_ARRAY
        val virtualFile = file.virtualFile ?: return PsiClass.EMPTY_ARRAY

        val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
        val javaClass = DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(file, virtualFile, classOrObject, project)
            ?: return PsiClass.EMPTY_ARRAY

        return arrayOf(javaClass)
    }
}
