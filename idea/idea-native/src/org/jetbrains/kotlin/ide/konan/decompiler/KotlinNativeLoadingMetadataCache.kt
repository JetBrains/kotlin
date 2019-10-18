/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil.createConcurrentWeakValueMap
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import java.io.IOException
import java.util.*

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

    // ConcurrentWeakValueHashMap does not allow null values.
    private class CacheValue<T : Any>(val value: T?)

    private val packageFragmentCache = createConcurrentWeakValueMap<CacheKey, CacheValue<ProtoBuf.PackageFragment>>()
    private val moduleHeaderCache = createConcurrentWeakValueMap<CacheKey, CacheValue<KlibMetadataProtoBuf.Header>>()
    private val libraryVersioningCache = createConcurrentWeakValueMap<CacheKey, CacheValue<KonanLibraryVersioning>>()

    fun getCachedPackageFragment(packageFragmentFile: VirtualFile): ProtoBuf.PackageFragment? {
        check(packageFragmentFile.extension == KLIB_METADATA_FILE_EXTENSION) {
            "Not a package metadata file: $packageFragmentFile"
        }

        return packageFragmentCache.computeIfAbsent(CacheKey(packageFragmentFile)) {
            CacheValue(computePackageFragment(packageFragmentFile))
        }.value
    }

    fun getCachedModuleHeader(moduleHeaderFile: VirtualFile): KlibMetadataProtoBuf.Header? {
        check(moduleHeaderFile.name == KLIB_MODULE_METADATA_FILE_NAME) {
            "Not a module header file: $moduleHeaderFile"
        }

        return moduleHeaderCache.computeIfAbsent(CacheKey(moduleHeaderFile)) {
            CacheValue(computeModuleHeader(moduleHeaderFile))
        }.value
    }

    private fun isAbiCompatible(libraryRoot: VirtualFile): Boolean {
        val manifestFile = libraryRoot.findChild(KLIB_MANIFEST_FILE_NAME) ?: return false

        val versioning = libraryVersioningCache.computeIfAbsent(CacheKey(manifestFile)) {
            CacheValue(computeLibraryVersioning(manifestFile))
        }.value

        return versioning?.abiVersion == KotlinAbiVersion.CURRENT
    }

    private fun computePackageFragment(packageFragmentFile: VirtualFile): ProtoBuf.PackageFragment? {
        if (!isAbiCompatible(packageFragmentFile.parent.parent.parent))
            return null

        return try {
            parsePackageFragment(packageFragmentFile.contentsToByteArray(false))
        } catch (_: IOException) {
            null
        }
    }

    private fun computeModuleHeader(moduleHeaderFile: VirtualFile): KlibMetadataProtoBuf.Header? {
        if (!isAbiCompatible(moduleHeaderFile.parent.parent))
            return null

        return try {
            parseModuleHeader(moduleHeaderFile.contentsToByteArray(false))
        } catch (_: IOException) {
            null
        }
    }

    private fun computeLibraryVersioning(manifestFile: VirtualFile): KonanLibraryVersioning? = try {
        Properties().apply { manifestFile.inputStream.use { load(it) } }.readKonanLibraryVersioning()
    } catch (_: IOException) {
        // ignore and cache null value
        null
    } catch (_: IllegalArgumentException) {
        // ignore and cache null value
        null
    }
}
