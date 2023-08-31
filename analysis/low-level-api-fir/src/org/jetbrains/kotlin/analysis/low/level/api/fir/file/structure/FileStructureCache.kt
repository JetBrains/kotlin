/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Belongs to a [org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession]
 */
internal class FileStructureCache(private val moduleResolveComponents: LLFirModuleResolveComponents) {
    private val cache = ConcurrentHashMap<KtFile, FileStructure>()

    fun getFileStructure(ktFile: KtFile): FileStructure = cache.computeIfAbsent(ktFile) {
        FileStructure.build(ktFile, moduleResolveComponents)
    }

    fun getCachedFileStructure(ktFile: KtFile): FileStructure? = cache[ktFile]
}