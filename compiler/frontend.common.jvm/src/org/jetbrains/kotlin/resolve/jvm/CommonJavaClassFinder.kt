/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.JavaAnnotationProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName

abstract class CommonJavaClassFinder : JavaClassFinder {
    protected abstract val javaFacade: KotlinJavaPsiFacade
    protected abstract val javaSearchScope: GlobalSearchScope
    protected abstract val annotationProvider: JavaAnnotationProvider?

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
}
