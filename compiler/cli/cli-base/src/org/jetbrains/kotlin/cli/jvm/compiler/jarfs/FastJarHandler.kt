/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

class FastJarHandler(val fileSystem: FastJarFileSystem, path: String) {
    private val archive: JarArchive = fileSystem.archives[path]

    /** The archive path only for the VFS-based implementation [FastJarVirtualFile]. */
    internal val path: Path
        get() = archive.path

    private val myRoot: VirtualFile = archive.buildTree(VirtualFileFactory())

    private inner class VirtualFileFactory : JarTreeFactory<FastJarVirtualFile> {
        override fun createRoot(): FastJarVirtualFile =
            FastJarVirtualFile(this@FastJarHandler, "", -1, parent = null, entryDescription = null)

        override fun createDirectory(name: CharSequence, parent: FastJarVirtualFile): FastJarVirtualFile =
            FastJarVirtualFile(this@FastJarHandler, name, -1, parent, entryDescription = null)

        override fun createFile(name: CharSequence, parent: FastJarVirtualFile, entry: ZipEntryDescription): FastJarVirtualFile =
            FastJarVirtualFile(this@FastJarHandler, name, entry.uncompressedSize, parent, entry)

        override fun setChildren(directory: FastJarVirtualFile, children: List<FastJarVirtualFile>) {
            directory.initChildren(children)
        }
    }

    fun findFileByPath(pathInJar: String): VirtualFile? {
        return myRoot.findFileByRelativePath(pathInJar)
    }

    fun contentsToByteArray(zipEntryDescription: ZipEntryDescription): ByteArray {
        return archive.contentsToByteArray(zipEntryDescription)
    }
}
