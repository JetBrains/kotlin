/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.github.benmanes.caffeine.cache.Caffeine
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsOnlyApi
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.ThreadSafe

/**
 * Caches the [KtFile] to [FirFile] mapping of a [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule].
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

    @LLStatisticsOnlyApi
    abstract fun getAllCachedFirFiles(): Collection<FirFile>
}

internal class ModuleFileCacheImpl(override val moduleComponents: LLFirModuleResolveComponents) : ModuleFileCache() {
    private val firFileCache = Caffeine.newBuilder()
        .weakValues()
        .build<KtFile, FirFile>()

    override fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile =
        firFileCache.get(file) { createValue() }!!

    override fun getCachedFirFile(ktFile: KtFile): FirFile? = firFileCache.getIfPresent(ktFile)

    override fun getContainerFirFile(declaration: FirDeclaration): FirFile? {
        // TODO (marco): Is this legal? Is it even a shortcut given that the attribute access probably also is a map access?
//        declaration.backReferencedFirFile?.let { return it }

        val ktFile = declaration.psi?.containingFile as? KtFile ?: return null

        // We have to recreate the file on demand, since a FIR file might be thrown away before a FIR declaration now, and resolution might
        // request the containing file.
        //
        // Before "FIR as Data," the containing file was guaranteed to exist, since files lived for the whole session.
        //
        // Note: `buildRawFirFileWithCaching` caches the new FIR file in this same cache, since it uses the cache under the hood.
        //
        // TODO (marco): This shouldn't be needed with back references to FIR, since as long as there is a declaration for which we want to
        //  get a containing file, the FIR file should still live via the declaration. But this would be a pretty good sanity check, so we
        //  could turn it into an assertion for testing.
        //
        // NOTE: Apparently this causes some dangling file tests to fail due to inconsistent modules, such as
        // `FirSourceLikeLazyDeclarationResolveTestGenerated.DanglingFile.IgnoreSelf#testRegularFunction`. When loading a FIR file for the
        // NON-DANGLING `KtFile`, the session's module is the dangling file and the actual module of the file is the source module. So we
        // are somehow trying to get the containing FIR file for a declaration from the source module, and building the FIR file for it is
        // not legal. It probably has something to do with `IGNORE_SELF`.
        return getCachedFirFile(ktFile)// ?: moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
    }

    @LLStatisticsOnlyApi
    override fun getAllCachedFirFiles(): Collection<FirFile> = firFileCache.asMap().values
}
