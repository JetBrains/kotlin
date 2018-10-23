/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumDataDescriptor
import org.jetbrains.kotlin.idea.caches.FileAttributeService
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment

enum class KnownLibraryKindForIndex {
    COMMON, JS, UNKNOWN
}

private val KOTLIN_LIBRARY_KIND_FILE_ATTRIBUTE: String = "kotlin-library-kind".apply {
    ServiceManager.getService(FileAttributeService::class.java)?.register(this, 1)
}

// TODO: Detect library kind for Jar file using IdePlatformKindResolution.
fun VirtualFile.getLibraryKindForJar(): KnownLibraryKindForIndex {
    if (this !is VirtualFileWithId) return detectLibraryKindFromJarContentsForIndex(this)

    val service =
        ServiceManager.getService(FileAttributeService::class.java)
            ?: return detectLibraryKindFromJarContentsForIndex(this)

    service
        .readEnumAttribute(KOTLIN_LIBRARY_KIND_FILE_ATTRIBUTE, this, KnownLibraryKindForIndex::class.java)
        ?.let { return it.value }

    return detectLibraryKindFromJarContentsForIndex(this).also { newValue ->
        service.writeEnumAttribute(KOTLIN_LIBRARY_KIND_FILE_ATTRIBUTE, this, newValue)
    }
}

private fun detectLibraryKindFromJarContentsForIndex(jarRoot: VirtualFile): KnownLibraryKindForIndex {
    var result: KnownLibraryKindForIndex? = null
    VfsUtil.visitChildrenRecursively(jarRoot, object : VirtualFileVisitor<Unit>() {
        override fun visitFile(file: VirtualFile): Boolean =
            when (file.extension) {
                "class" -> false

                "kjsm" -> {
                    result = KnownLibraryKindForIndex.JS
                    false
                }

                MetadataPackageFragment.METADATA_FILE_EXTENSION -> {
                    result = KnownLibraryKindForIndex.COMMON
                    false
                }

                else -> true
            }
    })
    return result ?: KnownLibraryKindForIndex.UNKNOWN
}
