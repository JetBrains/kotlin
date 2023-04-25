/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import java.io.InputStream

class CtSymClassVirtualFile(
    private val parent: VirtualFile?,
    private val file: VirtualFile
): VirtualFile() {
    override fun getName() = file.name

    override fun getFileSystem() = file.fileSystem

    override fun getPath() = file.path

    override fun isWritable() = file.isWritable

    override fun isDirectory() = false

    override fun isValid() = file.isValid

    override fun getParent(): VirtualFile? = parent

    override fun getChildren(): Array<VirtualFile>? = null

    override fun getOutputStream(p0: Any?, p1: Long, p2: Long) = file.getOutputStream(p0, p1, p2)

    override fun contentsToByteArray(): ByteArray = file.contentsToByteArray()

    override fun getTimeStamp() = file.timeStamp

    override fun getLength() = file.length

    override fun refresh(p0: Boolean, p1: Boolean, p2: Runnable?) = file.refresh(p0, p1, p2)

    override fun getInputStream(): InputStream = file.inputStream

    override fun getFileType(): FileType = JavaClassFileType.INSTANCE

    override fun getModificationStamp(): Long = file.modificationStamp
}
