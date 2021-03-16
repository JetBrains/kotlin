/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.io.BufferExposingByteArrayInputStream
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class FastJarVirtualFile(
    private val myHandler: FastJarHandler,
    private val myName: CharSequence,
    private val myLength: Long,
    private val myTimestamp: Long,
    parent: FastJarVirtualFile?
) : VirtualFile() {
    private val myParent: VirtualFile? = parent
    private var myChildren = EMPTY_ARRAY
    fun setChildren(children: Array<VirtualFile>) {
        myChildren = children
    }

    override fun getName(): String {
        return myName.toString()
    }

    override fun getNameSequence(): CharSequence {
        return myName
    }

    override fun getFileSystem(): VirtualFileSystem {
        return myHandler.fileSystem
    }

    override fun getPath(): String {
        if (myParent == null) {
            return FileUtil.toSystemIndependentName(myHandler.file.path) + "!/"
        }
        val parentPath = myParent.path
        val answer = StringBuilder(parentPath.length + 1 + myName.length)
        answer.append(parentPath)
        if (answer[answer.length - 1] != '/') {
            answer.append('/')
        }
        answer.append(myName)
        return answer.toString()
    }

    override fun isWritable(): Boolean {
        return false
    }

    override fun isDirectory(): Boolean {
        return myLength < 0
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        return myParent
    }

    override fun getChildren(): Array<VirtualFile> {
        return myChildren
    }

    @Throws(IOException::class)
    override fun getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("JarFileSystem is read-only")
    }

    @Throws(IOException::class)
    override fun contentsToByteArray(): ByteArray {
        val pair: Couple<String> = FastJarFileSystem.splitPath(
            path
        )
        return myHandler.contentsToByteArray(pair.second)
    }

    override fun getTimeStamp(): Long {
        return myTimestamp
    }

    override fun getLength(): Long {
        return myLength
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return BufferExposingByteArrayInputStream(contentsToByteArray())
    }

    override fun getModificationStamp(): Long {
        return 0
    }
}
