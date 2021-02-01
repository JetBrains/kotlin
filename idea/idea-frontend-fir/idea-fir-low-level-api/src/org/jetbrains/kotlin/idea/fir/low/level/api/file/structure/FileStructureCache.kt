/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Belongs to a [org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState]
 */
internal class FileStructureCache(
    private val fileBuilder: FirFileBuilder,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
    private val collector: FirTowerDataContextCollector,
) {
    private val cache = ConcurrentHashMap<KtFile, FileStructure>()

    fun getFileStructure(ktFile: KtFile, moduleFileCache: ModuleFileCache): FileStructure = cache.computeIfAbsent(ktFile) {
        val firFile = fileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, lazyBodiesMode = false)
        FileStructure(ktFile, firFile, firLazyDeclarationResolver, fileBuilder, moduleFileCache, collector)
    }
}