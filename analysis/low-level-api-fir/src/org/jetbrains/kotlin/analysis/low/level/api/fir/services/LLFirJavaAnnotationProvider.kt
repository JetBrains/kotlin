/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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
                    ?: directory.findFile(PsiPackage.PACKAGE_INFO_CLS_FILE)
                val packageStatement = (file as? PsiJavaFile)?.packageStatement ?: continue
                val psiAnnotations = packageStatement.patchedAnnotationList?.annotations ?: continue
                for (psiAnnotation in psiAnnotations) {
                    val psiSource = javaElementSourceFactory.createPsiSource(psiAnnotation)
                    val javaAnnotation = JavaAnnotationImpl(psiSource)
                    add(javaAnnotation)
                }
            }
        }
    }
}

/**
 * Not all implementations of [PsiPackageStatement] used to have a proper implementation of [PsiPackageStatement.annotationList].
 * Specifically, [com.intellij.psi.impl.compiled.ClsPackageStatementImpl] was simply throwing an [UnsupportedOperationException]
 * before IJ Platform 252.1.
 *
 * This property provides a workaround by catching the exception and extracting annotations directly from the
 * `package-info.class` file's single class modifier list when available.
 *
 * The current version of IJ Platform dependency in Kotlin repo is 251 (see KT-80525).
 *
 * Starting from IJ Platform version 252.1, `ClsPackageStatementImpl` has a similar workaround built-in (see IDEA-375444).
 * And starting from IJ Platform version 253, `ClsPackageStatementImpl` properly supports `annotationList` (see IDEA-375067).
 *
 * When the IJ Platform dependency is updated to 252.1 or higher, this extension property can be removed - see KT-83480 for that task.
 */
private val PsiPackageStatement.patchedAnnotationList: PsiModifierList?
    get() = try {
        annotationList
    } catch (_: UnsupportedOperationException) {
        val file = containingFile

        if (file.name == PsiPackage.PACKAGE_INFO_CLS_FILE) {
            val singlePackageInfoClass = (file as? PsiClassOwner)?.classes?.singleOrNull()

            singlePackageInfoClass?.modifierList
        } else {
            null
        }
    }
