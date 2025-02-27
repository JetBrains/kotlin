/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.localfs

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.load.kotlin.FileThatCanBeCachedAcrossEntireApp
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.*

class KotlinLocalVirtualFile(
    val file: File,
    private val _fileSystem: KotlinLocalFileSystem,
    parent: KotlinLocalVirtualFile? = null,
) : VirtualFile(), FileThatCanBeCachedAcrossEntireApp {
    // File system operations can be slow, so we're caching high-impact properties.
    private var _name: String? = null
    private var _parent: KotlinLocalVirtualFile? = parent

    private val _children: Pair<Array<KotlinLocalVirtualFile>, Map<String, KotlinLocalVirtualFile>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val fileChildren = file.listFiles() ?: emptyArray()
        val array = fileChildren.map { KotlinLocalVirtualFile(it, _fileSystem, parent = this) }.toTypedArray()
        val map = array.associateBy { it.name }
        array to map
    }

    private var _isDirectory: Boolean = file.isDirectory
    private val _timeStamp = file.lastModified()

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
        return _isDirectory
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        _parent?.let { return it }
        val parentFile = file.parentFile ?: return null
        return KotlinLocalVirtualFile(parentFile, _fileSystem).also {
            _parent = it
        }
    }

    override fun getChildren(): Array<KotlinLocalVirtualFile> = _children.first

    override fun findChild(name: String): VirtualFile? = _children.second[name]

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        shouldNotBeCalled()
    }

    override fun contentsToByteArray(): ByteArray {
        return FileUtil.loadFileBytes(file)
    }

    override fun getTimeStamp(): Long {
        return _timeStamp
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
     *   different instances.
     *
     * Two directories with the same path are always equal.
     */
    override fun equals(other: Any?): Boolean {
        return other is KotlinLocalVirtualFile && file == other.file && (_isDirectory || _timeStamp == other._timeStamp)
    }

    override fun hashCode(): Int {
        return 31 * file.hashCode() + (if (_isDirectory) 0 else _timeStamp.hashCode())
    }
}
