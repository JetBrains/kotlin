/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderBaseImpl
import org.jetbrains.kotlin.psi.KtPlatformInterface
import java.io.ByteArrayInputStream
import java.net.URL

@OptIn(KtPlatformInterface::class)
@KaImplementationDetail
class BuiltinsVirtualFileProviderCliImpl : BuiltinsVirtualFileProviderBaseImpl() {
    private val fileSystem = BuiltinVirtualFileSystem { files }
    private val files: Set<VirtualFile> by lazy { super.getBuiltinVirtualFiles() }

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    override fun findVirtualFile(url: URL): VirtualFile {
        // With the default caching, the JDK would keep the JAR of the resource open
        val content = url.openConnection().apply { useCaches = false }.getInputStream().use { it.readBytes() }
        return BuiltinVirtualFile(fileSystem, url.path.substringAfterLast('/'), content)
    }
}

/**
 * Holds in-memory copies of built-in files, so that they don't clash with regular virtual files of the same `kotlin-stdlib` JAR
 * when it is also a library of the analyzed project. This typically only happens in the Analysis API tests.
 */
private class BuiltinVirtualFileSystem(filesFactory: () -> Set<VirtualFile>) : DeprecatedVirtualFileSystem(), NonPhysicalFileSystem {
    private val filesByPath by lazy(LazyThreadSafetyMode.PUBLICATION) { filesFactory().associateBy { it.path } }

    override fun getProtocol() = "kotlin-builtins"

    override fun findFileByPath(path: String) = filesByPath[path]
    override fun refreshAndFindFileByPath(path: String) = findFileByPath(path)

    override fun refresh(asynchronous: Boolean) {}
}

private class BuiltinVirtualFile(
    private val fileSystem: BuiltinVirtualFileSystem,
    private val name: String,
    private val content: ByteArray,
) : VirtualFile() {
    override fun getName() = name
    override fun getFileSystem() = fileSystem
    override fun getPath() = "/$name"

    override fun isWritable() = false
    override fun isDirectory() = false
    override fun isValid() = true

    override fun getParent() = null
    override fun getChildren(): Array<VirtualFile> = EMPTY_ARRAY

    override fun contentsToByteArray() = content
    override fun getInputStream() = ByteArrayInputStream(content)
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) = throw UnsupportedOperationException()

    override fun getLength() = content.size.toLong()
    override fun getTimeStamp() = 0L
    override fun getModificationStamp() = 0L

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
}
