/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Belongs to a [org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState]
 */
internal class FileStructureCache(
    private val fileBuilder: FirFileBuilder,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
) {
    private val cache = ConcurrentHashMap<KtFile, FileStructure>()

    fun getFileStructure(ktFile: KtFile, moduleFileCache: ModuleFileCache): FileStructure = cache.computeIfAbsent(ktFile) {
        FileStructure.build(ktFile, firLazyDeclarationResolver, fileBuilder, moduleFileCache)
    }
}