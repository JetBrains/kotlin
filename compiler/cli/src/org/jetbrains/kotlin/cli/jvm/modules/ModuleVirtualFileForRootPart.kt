/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.vfs.VirtualFile
import java.io.InputStream

class ModuleVirtualFileForRootPart(
    private val parent: VirtualFile?,
    private val virtualFile: VirtualFile,
    private val packages: Map<String, Boolean>,
    private val currentPackage: String
) : VirtualFile() {

    private val _children by lazy {
        val children = virtualFile.children ?: return@lazy null
        val isExported = packages.getOrDefault(currentPackage, false)
        children.mapNotNull {
            when {
                it.isDirectory -> {
                    val childPackage = if (currentPackage.isEmpty()) it.name else currentPackage + "." + it.name
                    if (packages.contains(childPackage))
                        ModuleVirtualFileForRootPart(this, it, packages, childPackage)
                    else null
                }
                isExported -> ModuleVirtualFileForRootPart(this, it, emptyMap(), currentPackage)
                else -> null
            }
        }.toTypedArray<VirtualFile>()
    }

    override fun getName() = virtualFile.name

    override fun getFileSystem() = virtualFile.fileSystem

    override fun getPath() = virtualFile.path

    override fun isWritable() = virtualFile.isWritable

    override fun isDirectory() = virtualFile.isDirectory

    override fun isValid() = virtualFile.isValid

    override fun getParent(): VirtualFile? = parent

    override fun getChildren(): Array<VirtualFile>? = _children

    override fun getOutputStream(p0: Any?, p1: Long, p2: Long) = virtualFile.getOutputStream(p0, p1, p2)

    override fun contentsToByteArray(): ByteArray = virtualFile.contentsToByteArray()

    override fun getTimeStamp() = virtualFile.timeStamp

    override fun getLength() = virtualFile.length

    override fun refresh(p0: Boolean, p1: Boolean, p2: Runnable?) = virtualFile.refresh(p0, p1, p2)

    override fun getInputStream(): InputStream? = virtualFile.inputStream
}