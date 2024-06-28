/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.util.io.URLUtil
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

internal class CoreJrtVirtualFile(
    private val virtualFileSystem: CoreJrtFileSystem,
    private val jdkHomePath: String,
    private val path: Path,
    private val parent: CoreJrtVirtualFile?,
) : VirtualFile() {
    // TODO: catch IOException?
    private val attributes: BasicFileAttributes get() = Files.readAttributes(path, BasicFileAttributes::class.java)

    override fun getFileSystem(): VirtualFileSystem = virtualFileSystem

    override fun getName(): String =
        path.fileName.toString()

    override fun getPath(): String =
        FileUtil.toSystemIndependentName(jdkHomePath + URLUtil.JAR_SEPARATOR + path)

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = Files.isDirectory(path)

    override fun isValid(): Boolean = true

    override fun getParent(): VirtualFile? = parent

    private val myChildren by lazy { computeChildren() }

    override fun getChildren(): Array<out VirtualFile> = myChildren

    private fun computeChildren(): Array<out VirtualFile> {
        val paths = try {
            Files.newDirectoryStream(path).use(Iterable<Path>::toList)
        } catch (e: IOException) {
            emptyList<Path>()
        }
        return when {
            paths.isEmpty() -> EMPTY_ARRAY
            else -> paths.map { path -> CoreJrtVirtualFile(virtualFileSystem, jdkHomePath, path, parent = this) }.toTypedArray()
        }
    }

    override fun getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
        throw UnsupportedOperationException()

    override fun contentsToByteArray(): ByteArray =
        Files.readAllBytes(path)

    override fun getTimeStamp(): Long =
        attributes.lastModifiedTime().toMillis()

    override fun getLength(): Long = attributes.size()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    override fun getInputStream(): InputStream =
        VfsUtilCore.inputStreamSkippingBOM(Files.newInputStream(path).buffered(), this)

    override fun getModificationStamp(): Long = 0

    override fun equals(other: Any?): Boolean =
        other is CoreJrtVirtualFile && path == other.path && fileSystem == other.fileSystem

    override fun hashCode(): Int =
        path.hashCode()
}
