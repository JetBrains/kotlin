/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.caches

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.*
import org.jetbrains.kotlin.analysis.utils.collections.ConcurrentMapBasedCache
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.light.classes.symbol.FirLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.decompiled.KtLightClassForDecompiledFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap

class SymbolLightClassFacadeCache(private val project: Project) {
    private val cache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentMapBasedCache<FacadeKey, KtLightClassForFacade?>(ConcurrentHashMap())
    }

    fun getOrCreateSymbolLightFacade(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): KtLightClassForFacade? {
        if (ktFiles.isEmpty()) return null
        val key = FacadeKey(facadeClassFqName, ktFiles.toSet())
        return cache.getOrPut(key) {
            getOrCreateFirLightFacadeNoCache(ktFiles, facadeClassFqName)
        }
    }

    private fun getOrCreateFirLightFacadeNoCache(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): KtLightClassForFacade? {
        val firstFile = ktFiles.first()
        return when {
            ktFiles.none { it.isCompiled } ->
                return FirLightClassForFacade(firstFile.manager, facadeClassFqName, ktFiles)
            ktFiles.all { it.isCompiled } -> {
                val file = ktFiles.firstOrNull { it.javaFileFacadeFqName == facadeClassFqName } as? KtClsFile
                    ?: error("Can't find the representative decompiled file for $facadeClassFqName")
                val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
                val clsDelegate = DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(
                    mirrorFile = file,
                    classFile = file.virtualFile,
                    correspondingClassOrObject = classOrObject,
                    project = project,
                ) ?: return null
                KtLightClassForDecompiledFacade(clsDelegate, clsDelegate.parent, file, classOrObject, ktFiles)
            }
            else ->
                error("Source and compiled files are mixed: $ktFiles}")
        }
    }

    private data class FacadeKey(val fqName: FqName, val files: Set<KtFile>)
}