/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.library.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.library.readKonanLibraryVersioning
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import java.io.IOException
import java.util.*

@Service
class KlibLoadingMetadataCache {
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

    private val packageFragmentCache = ContainerUtil.createConcurrentWeakValueMap<CacheKey, CacheValue<ProtoBuf.PackageFragment>>()
    private val moduleHeaderCache = ContainerUtil.createConcurrentWeakValueMap<CacheKey, CacheValue<KlibMetadataProtoBuf.Header>>()
    private val libraryMetadataVersionCache = ContainerUtil.createConcurrentWeakValueMap<CacheKey, CacheValue<KlibMetadataVersion>>()

    fun getCachedPackageFragment(packageFragmentFile: VirtualFile): ProtoBuf.PackageFragment? {
        check(packageFragmentFile.extension == KLIB_METADATA_FILE_EXTENSION) {
            "Not a package metadata file: $packageFragmentFile"
        }

        return packageFragmentCache.computeIfAbsent(
            CacheKey(packageFragmentFile)
        ) {
            CacheValue(computePackageFragment(packageFragmentFile))
        }.value
    }

    fun getCachedModuleHeader(moduleHeaderFile: VirtualFile): KlibMetadataProtoBuf.Header? {
        check(moduleHeaderFile.name == KLIB_MODULE_METADATA_FILE_NAME) {
            "Not a module header file: $moduleHeaderFile"
        }

        return moduleHeaderCache.computeIfAbsent(
            CacheKey(moduleHeaderFile)
        ) {
            CacheValue(computeModuleHeader(moduleHeaderFile))
        }.value
    }

    fun getCachedPackageFragmentWithVersion(packageFragmentFile: VirtualFile): Pair<ProtoBuf.PackageFragment?, KlibMetadataVersion?> {
        val packageFragment = getCachedPackageFragment(packageFragmentFile) ?: return null to null
        val version = getCachedMetadataVersion(getKlibLibraryRootForPackageFragment(packageFragmentFile))
        return packageFragment to version
    }

    private fun getCachedMetadataVersion(libraryRoot: VirtualFile): KlibMetadataVersion? {
        val manifestFile = libraryRoot.findChild(KLIB_MANIFEST_FILE_NAME) ?: return null

        val metadataVersion = libraryMetadataVersionCache.computeIfAbsent(
            CacheKey(manifestFile)
        ) {
            CacheValue(computeLibraryMetadataVersion(manifestFile))
        }.value

        return metadataVersion
    }

    private fun getKlibLibraryRootForPackageFragment(packageFragmentFile: VirtualFile): VirtualFile {
        return packageFragmentFile.parent.parent.parent
    }

    private fun isMetadataCompatible(libraryRoot: VirtualFile): Boolean {
        val metadataVersion = getCachedMetadataVersion(libraryRoot) ?: return false

        return metadataVersion.isCompatibleWithCurrentCompilerVersion()
    }

    private fun computePackageFragment(packageFragmentFile: VirtualFile): ProtoBuf.PackageFragment? {
        if (!isMetadataCompatible(getKlibLibraryRootForPackageFragment(packageFragmentFile)))
            return null

        return try {
            parsePackageFragment(packageFragmentFile.contentsToByteArray(false))
        } catch (_: IOException) {
            null
        }
    }

    private fun computeModuleHeader(moduleHeaderFile: VirtualFile): KlibMetadataProtoBuf.Header? {
        if (!isMetadataCompatible(moduleHeaderFile.parent.parent))
            return null

        return try {
            parseModuleHeader(moduleHeaderFile.contentsToByteArray(false))
        } catch (_: IOException) {
            null
        }
    }

    private fun computeLibraryMetadataVersion(manifestFile: VirtualFile): KlibMetadataVersion? = try {
        val versioning = Properties().apply { manifestFile.inputStream.use { load(it) } }.readKonanLibraryVersioning()
        versioning.metadataVersion?.let(BinaryVersion.Companion::parseVersionArray)?.let(::KlibMetadataVersion)
    } catch (_: IOException) {
        // ignore and cache null value
        null
    } catch (_: IllegalArgumentException) {
        // ignore and cache null value
        null
    }

    companion object {
        @JvmStatic
        fun getInstance(): KlibLoadingMetadataCache =
            ApplicationManager.getApplication().getService(KlibLoadingMetadataCache::class.java)
    }

}
