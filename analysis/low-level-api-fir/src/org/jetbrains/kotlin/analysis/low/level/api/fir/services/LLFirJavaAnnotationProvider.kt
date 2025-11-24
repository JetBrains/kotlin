/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.caches.getOrPut
import org.jetbrains.kotlin.fir.java.FirJavaAnnotationProvider
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName

internal class LLFirJavaAnnotationProvider(
    private val project: Project,
    private val scope: GlobalSearchScope,
) : FirJavaAnnotationProvider {
    private val javaElementSourceFactory: JavaElementSourceFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        JavaElementSourceFactory.getInstance(project)
    }

    private val packageAnnotationCache: Cache<FqName, List<JavaAnnotation>> = Caffeine
        .newBuilder()
        .maximumSize(1024)
        .build()

    override fun getPackageAnnotations(owner: JavaPackage): List<JavaAnnotation> {
        require(owner is JavaPackageImpl)
        return packageAnnotationCache.getOrPut(owner.fqName) {
            computePackageAnnotations(owner.psi)
        }
    }

    private fun computePackageAnnotations(psi: PsiPackage): List<JavaAnnotation> {
        val directories = psi.getDirectories(scope)
        if (directories.isEmpty()) {
            return emptyList()
        }

        return buildList {
            for (directory in directories) {
                val file = directory.findFile(PsiPackage.PACKAGE_INFO_FILE)
                val packageStatement = (file as? PsiJavaFile)?.packageStatement ?: continue
                val psiAnnotations = packageStatement.annotationList?.annotations ?: continue
                for (psiAnnotation in psiAnnotations) {
                    val psiSource = javaElementSourceFactory.createPsiSource(psiAnnotation)
                    val javaAnnotation = JavaAnnotationImpl(psiSource)
                    add(javaAnnotation)
                }
            }
        }
    }
}
