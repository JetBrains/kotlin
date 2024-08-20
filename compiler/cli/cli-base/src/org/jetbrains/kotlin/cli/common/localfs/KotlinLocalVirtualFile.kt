/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.localfs

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.*

class KotlinLocalVirtualFile(
    val file: File,
    private val _fileSystem: KotlinLocalFileSystem,
) : VirtualFile() {
    // File system operations can be slow, so we're caching high-impact properties.
    private var _name: String? = null

    private var _isDirectory: Boolean? = null

    private var _children: Array<VirtualFile>? = null

    override fun getName(): String {
        _name?.let { return it }
        return file.name.also { _name = it }
    }

    override fun getFileSystem(): VirtualFileSystem {
        return _fileSystem
    }

    override fun getPath(): String {
        return FileUtil.toSystemIndependentName(file.absolutePath)
    }

    override fun isWritable(): Boolean {
        return false
    }

    override fun isDirectory(): Boolean {
        _isDirectory?.let { return it }
        return file.isDirectory.also { _isDirectory = it }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        val parentFile = file.parentFile ?: return null
        return KotlinLocalVirtualFile(parentFile, _fileSystem)
    }

    override fun getChildren(): Array<VirtualFile> {
        _children?.let { return it }
        val fileChildren = file.listFiles() ?: emptyArray()

        _children = fileChildren
            .map { KotlinLocalVirtualFile(it, _fileSystem) }
            .sortedBy { it.name }
            .toTypedArray<VirtualFile>()

        return _children!!
    }

    override fun findChild(name: String): VirtualFile? = children.binarySearchBy(name) { it.name }

    /**
     * The implementation is copied from [List.binarySearch] and changed to fit our needs.
     *
     * We cannot use [binarySearchBy][kotlin.collections.binarySearchBy] here because we need to search in an array. And we cannot use
     * [Arrays.binarySearch][java.util.Arrays.binarySearch] because it doesn't support searching by a key instead of the whole element.
     */
    private inline fun <T, K : Comparable<K>> Array<T>.binarySearchBy(key: K, selector: (T) -> K): T? {
        var low = 0
        var high = size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = get(mid)
            val cmp = compareValues(selector(midVal), key)

            if (cmp < 0) {
                low = mid + 1
            } else if (cmp > 0) {
                high = mid - 1
            } else {
                return midVal
            }
        }

        return null
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        shouldNotBeCalled()
    }

    override fun contentsToByteArray(): ByteArray {
        return FileUtil.loadFileBytes(file)
    }

    override fun getTimeStamp(): Long {
        return file.lastModified()
    }

    override fun getLength(): Long {
        return file.length()
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    override fun getInputStream(): InputStream {
        return VfsUtilCore.inputStreamSkippingBOM(BufferedInputStream(FileInputStream(file)), this)
    }

    override fun getModificationStamp(): Long {
        return 0
    }

    override fun isInLocalFileSystem(): Boolean {
        return true
    }

    /**
     * [KotlinLocalVirtualFile] is a transparent view to the file-system, so it doesn't
     *   matter if two files came from the same instance of [KotlinLocalFileSystem] or
     *   different instances
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinLocalVirtualFile

        return file == other.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}
