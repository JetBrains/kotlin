/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.KotlinLibraryImpl
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.metadata.ProtoBuf

object CachingIdeKlibMetadataLoader : PackageAccessHandler {
    override fun loadModuleHeader(library: KotlinLibrary): KlibMetadataProtoBuf.Header {
        val virtualFile = getVirtualFile(library, library.moduleHeaderFile)
        return virtualFile?.let { cache.getCachedModuleHeader(virtualFile) } ?: KlibMetadataProtoBuf.Header.getDefaultInstance()
    }

    override fun loadPackageFragment(library: KotlinLibrary, packageFqName: String, partName: String): ProtoBuf.PackageFragment {
        val virtualFile = getVirtualFile(library, library.packageFragmentFile(packageFqName, partName))
        return virtualFile?.let { cache.getCachedPackageFragment(virtualFile) } ?: ProtoBuf.PackageFragment.getDefaultInstance()
    }

    private fun getVirtualFile(library: KotlinLibrary, file: KFile): VirtualFile? =
        if (library.isZipped) asJarFileSystemFile(library.libraryFile, file) else asLocalFile(file)

    private fun asJarFileSystemFile(jarFile: KFile, localFile: KFile): VirtualFile? {
        val fullPath = jarFile.absolutePath + "!" + PathUtil.toSystemIndependentName(localFile.path)
        return StandardFileSystems.jar().findFileByPath(fullPath)
    }

    private fun asLocalFile(localFile: KFile): VirtualFile? {
        val fullPath = localFile.absolutePath
        return StandardFileSystems.local().findFileByPath(fullPath)
    }

    private val cache
        get() = KlibLoadingMetadataCache.getInstance()

    private val KotlinLibrary.moduleHeaderFile
        get() = (this as KotlinLibraryImpl).metadata.access.layout.moduleHeaderFile

    private fun KotlinLibrary.packageFragmentFile(packageFqName: String, partName: String) =
        (this as KotlinLibraryImpl).metadata.access.layout.packageFragmentFile(packageFqName, partName)

    private val KotlinLibrary.isZipped
        get() = (this as KotlinLibraryImpl).base.access.layout.isZipped
}
