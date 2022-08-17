/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.caches

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.getValue
import org.jetbrains.kotlin.analysis.utils.caches.softCachedValue
import org.jetbrains.kotlin.analysis.utils.collections.ConcurrentMapBasedCache
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.analyzeForLightClasses
import org.jetbrains.kotlin.name.FqName
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
        val ktFilesWithoutScript = ktFiles.filterNot { it.isScript() }
        if (ktFilesWithoutScript.isEmpty()) return null
        val key = FacadeKey(facadeClassFqName, ktFilesWithoutScript.toSet())
        return cache.getOrPut(key) {
            getOrCreateSymbolLightFacadeNoCache(ktFilesWithoutScript, facadeClassFqName)
        }
    }

    private fun getOrCreateSymbolLightFacadeNoCache(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): KtLightClassForFacade? {
        val firstFile = ktFiles.first()
        return when {
            ktFiles.none { it.isCompiled } ->
                analyzeForLightClasses(firstFile) {
                    SymbolLightClassForFacade(firstFile.manager, facadeClassFqName, ktFiles)
                }

            ktFiles.all { it.isCompiled } -> {
                DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeClassFqName, ktFiles)
            }

            else ->
                error("Source and compiled files are mixed: $ktFiles}")
        }
    }

    private data class FacadeKey(val fqName: FqName, val files: Set<KtFile>)
}