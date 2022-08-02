/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.google.common.collect.MapMaker
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import java.util.concurrent.ConcurrentMap

/**
 * Caches mapping [KtFile] -> [FirFile] of module [KtModule]
 */
@ThreadSafe
internal abstract class ModuleFileCache {
    abstract val moduleComponents: LLFirModuleResolveComponents

    /**
     * @return [FirFile] by [file] if it was previously built or runs [createValue] otherwise
     * The [createValue] is run under the lock so [createValue] is executed at most once for each [KtFile]
     */
    abstract fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile

    abstract fun getContainerFirFile(declaration: FirDeclaration): FirFile?

    abstract fun getCachedFirFile(ktFile: KtFile): FirFile?
}

internal class ModuleFileCacheImpl(override val moduleComponents: LLFirModuleResolveComponents) : ModuleFileCache() {
    private val ktFileToFirFile: ConcurrentMap<KtFile, FirFile> = MapMaker().weakKeys().makeMap()
    override fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile =
        ktFileToFirFile.computeIfAbsent(file) { createValue() }

    override fun getCachedFirFile(ktFile: KtFile): FirFile? = ktFileToFirFile[ktFile]

    override fun getContainerFirFile(declaration: FirDeclaration): FirFile? {
        val ktFile = declaration.psi?.containingFile as? KtFile ?: return null
        return getCachedFirFile(ktFile)
    }
}
