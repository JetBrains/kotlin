/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil.createConcurrentWeakValueMap
import org.jetbrains.kotlin.konan.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.library.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf

class KotlinNativeLoadingMetadataCache : BaseComponent {

    companion object {
        @JvmStatic
        fun getInstance(): KotlinNativeLoadingMetadataCache =
            ApplicationManager.getApplication().getComponent(KotlinNativeLoadingMetadataCache::class.java)
    }

    // Use special CacheKey class instead of VirtualFile for cache keys. Certain types of VirtualFiles (for example, obtained from JarFileSystem)
    // do not compare path (url) and modification stamp in equals() method.
    private data class CacheKey(
        val url: String,
        val modificationStamp: Long
    ) {
        constructor(virtualFile: VirtualFile) : this(virtualFile.url, virtualFile.modificationStamp)
    }

    private val packageFragmentCache = createConcurrentWeakValueMap<CacheKey, ProtoBuf.PackageFragment>()
    private val moduleHeaderCache = createConcurrentWeakValueMap<CacheKey, KlibMetadataProtoBuf.Header>()

    fun getCachedPackageFragment(virtualFile: VirtualFile): ProtoBuf.PackageFragment =
        packageFragmentCache.computeIfAbsent(CacheKey(virtualFile.ensurePackageMetadataFile)) {
            virtualFile.createPackageFragmentCacheEntry
        }

    fun getCachedModuleHeader(virtualFile: VirtualFile): KlibMetadataProtoBuf.Header =
        moduleHeaderCache.computeIfAbsent(CacheKey(virtualFile.ensureModuleHeaderFile)) {
            virtualFile.createModuleHeaderCacheEntry
        }

    private val VirtualFile.createPackageFragmentCacheEntry
        get() = parsePackageFragment(contentsToByteArray(false))

    private val VirtualFile.createModuleHeaderCacheEntry
        get() = parseModuleHeader(contentsToByteArray(false))

    private val VirtualFile.ensurePackageMetadataFile
        get() = if (isPackageMetadataFile) this else error("Not a package metadata file: $this")

    private val VirtualFile.isPackageMetadataFile
        get() = extension == KLIB_METADATA_FILE_EXTENSION

    private val VirtualFile.ensureModuleHeaderFile
        get() = if (isModuleHeaderFile) this else error("Not a module header file: $this")

    private val VirtualFile.isModuleHeaderFile
        get() = name == KLIB_MODULE_METADATA_FILE_NAME
}
