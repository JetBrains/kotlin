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
    private var _children: Array<VirtualFile>? = null

    override fun getName(): String {
        return file.name
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
        return file.isDirectory
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
        _children = fileChildren.map { KotlinLocalVirtualFile(it, _fileSystem) }.toTypedArray<VirtualFile>()
        return _children!!
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
