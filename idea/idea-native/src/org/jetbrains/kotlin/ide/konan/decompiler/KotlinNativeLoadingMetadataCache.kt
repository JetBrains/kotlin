/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.VfsUtilCore.isEqualOrAncestor
import com.intellij.util.containers.ContainerUtil.createConcurrentSoftValueMap
import org.jetbrains.kotlin.konan.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.serialization.konan.parseModuleHeader
import org.jetbrains.kotlin.serialization.konan.parsePackageFragment

class KotlinNativeLoadingMetadataCache : ApplicationComponent {

    companion object {
        @JvmStatic
        fun getInstance(): KotlinNativeLoadingMetadataCache =
            ApplicationManager.getApplication().getComponent(KotlinNativeLoadingMetadataCache::class.java)
    }

    private val packageFragmentCache = createConcurrentSoftValueMap<String, KonanProtoBuf.LinkDataPackageFragment>()
    private val moduleHeaderCache = createConcurrentSoftValueMap<String, KonanProtoBuf.LinkDataLibrary>()

    fun getCachedPackageFragment(virtualFile: VirtualFile): KonanProtoBuf.LinkDataPackageFragment =
        packageFragmentCache.computeIfAbsent(virtualFile.ensurePackageMetadataFile.url) {
            virtualFile.createPackageFragmentCacheEntry
        }

    fun getCachedModuleHeader(virtualFile: VirtualFile): KonanProtoBuf.LinkDataLibrary =
        moduleHeaderCache.computeIfAbsent(virtualFile.ensureModuleHeaderFile.url) {
            virtualFile.createModuleHeaderCacheEntry
        }

    override fun initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileCreated(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun fileDeleted(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun fileMoved(event: VirtualFileMoveEvent) = invalidateCaches(event.file)
            override fun contentsChanged(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun propertyChanged(event: VirtualFilePropertyEvent) = invalidateCaches(event.file)
        })
    }

    private fun invalidateCaches(modifiedFile: VirtualFile) {
        when {
            modifiedFile.isPackageMetadataFile -> packageFragmentCache.remove(modifiedFile.url)
            modifiedFile.isModuleHeaderFile -> moduleHeaderCache.remove(modifiedFile.url)
            modifiedFile.isDirectory -> {
                val ancestorUrl = modifiedFile.url
                packageFragmentCache.removeDescendants(ancestorUrl)
                moduleHeaderCache.removeDescendants(ancestorUrl)
            }
        }
    }

    private fun <V> MutableMap<String, V>.removeDescendants(ancestorUrl: String) {

        // Warning: Do not try to apply ".removeIf {}" to "entries". This will have no effect to
        // the Map produced by ContainerUtil.createConcurrentSoftValueMap().
        keys.removeIf { descendantCandidateUrl -> isEqualOrAncestor(ancestorUrl, descendantCandidateUrl) }
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
