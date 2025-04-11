/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentMap

/**
 * Caches [FileStructure] instances for an [LLFirResolveSession][org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade].
 */
internal class FileStructureCache(private val moduleResolveComponents: LLFirModuleResolveComponents) {
    /**
     * File structure elements can be rebuilt at any time and do not need to be unique (like FIR symbols), so they can be soft-referenced
     * from this cache to reduce memory consumption. Any `analyze` call in a file causes file structure elements to be built, so during
     * operations which cause a lot of files to be analyzed (such as Find Usages), a session might accumulate a lot of file structure
     * elements.
     */
    private val cache: ConcurrentMap<KtFile, FileStructure> = ContainerUtil.createConcurrentSoftKeySoftValueMap()

    fun getFileStructure(ktFile: KtFile): FileStructure = cache.computeIfAbsent(ktFile) {
        FileStructure.build(ktFile, moduleResolveComponents)
    }

    fun getCachedFileStructure(ktFile: KtFile): FileStructure? = cache[ktFile]
}
