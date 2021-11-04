/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.caches

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.light.classes.symbol.FirLightClassForFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.analysis.utils.caches.*
import java.util.concurrent.ConcurrentHashMap

class SymbolLightClassFacadeCache(project: Project) {
    private val cache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentHashMap<FacadeKey, FirLightClassForFacade>()
    }

    fun getOrCreateSymbolLightFacade(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): FirLightClassForFacade? {
        if (ktFiles.isEmpty()) return null
        val key = FacadeKey(facadeClassFqName, ktFiles.toSet())
        return cache.computeIfAbsent(key) {
            getOrCreateFirLightFacadeNoCache(ktFiles, facadeClassFqName)
        }
    }

    private fun getOrCreateFirLightFacadeNoCache(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): FirLightClassForFacade {
        val firstFile = ktFiles.first()
        return FirLightClassForFacade(firstFile.manager, facadeClassFqName, ktFiles)
    }

    private data class FacadeKey(val fqName: FqName, val files: Set<KtFile>)
}