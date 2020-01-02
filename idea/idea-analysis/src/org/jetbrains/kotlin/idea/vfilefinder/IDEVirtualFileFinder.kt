/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.FileNotFoundException
import java.io.InputStream

class IDEVirtualFileFinder(private val scope: GlobalSearchScope) : VirtualFileFinder() {
    override fun findMetadata(classId: ClassId): InputStream? {
        val file = findVirtualFileWithHeader(classId, KotlinMetadataFileIndex.KEY) ?: return null

        return try {
            file.inputStream
        } catch (e: FileNotFoundException) {
            null
        }
    }

    override fun hasMetadataPackage(fqName: FqName): Boolean = KotlinMetadataFilePackageIndex.hasSomethingInPackage(fqName, scope)

    // TODO: load built-ins metadata from scope
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null

    init {
        if (scope != GlobalSearchScope.EMPTY_SCOPE && scope.project == null) {
            LOG.warn("Scope with null project $scope")
        }
    }

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? = findVirtualFileWithHeader(classId, KotlinClassFileIndex.KEY)

    private fun findVirtualFileWithHeader(classId: ClassId, key: ID<FqName, Void>): VirtualFile? =
        FileBasedIndex.getInstance().getContainingFiles(key, classId.asSingleFqName(), scope).firstOrNull()

    companion object {
        private val LOG = Logger.getInstance(IDEVirtualFileFinder::class.java)
    }
}
