/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.factories

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.io.ZipFileSystemAccessor
import org.jetbrains.kotlin.io.ZipFileSystemCacheableAccessor
import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * The [ZipFileSystemAccessor] for the KLib archives read by [LLBinaryOriginLibrarySymbolProviderFactory].
 *
 * A KLib's metadata is read part by part, e.g., a package fragment at a time, and without a cache, each read opens the ZIP file system
 * of the archive anew, which reads the whole central directory of the archive. Like the compiler, this accessor keeps the recently used
 * archives open instead.
 *
 * The archives are closed when the project is disposed.
 */
internal class LLKlibZipFileSystemAccessor : ZipFileSystemAccessor, Disposable {
    private val accessor = ZipFileSystemCacheableAccessor(CACHE_LIMIT)

    override fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T =
        accessor.withZipFileSystem(zipFile, action)

    override fun dispose() {
        accessor.reset()
    }

    companion object {
        /**
         * The default of the compiler's `-Xklib-zip-file-accessor-cache-limit`.
         */
        private const val CACHE_LIMIT = 64

        fun getInstance(project: Project): LLKlibZipFileSystemAccessor = project.service()
    }
}
