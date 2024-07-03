/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.localfs

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.nio.file.Path

class KotlinLocalFileSystem : CoreLocalFileSystem() {
    override fun getProtocol(): String {
        return StandardFileSystems.FILE_PROTOCOL
    }

    override fun findFileByPath(path: String): VirtualFile? {
        return findFileByIoFile(File(path))
    }

    override fun refresh(asynchronous: Boolean) {}

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    override fun getNioPath(file: VirtualFile): Path? {
        return (file as? KotlinLocalVirtualFile)?.file?.toPath()
    }

    override fun findFileByIoFile(file: File): VirtualFile? {
        return runIf(file.exists()) { KotlinLocalVirtualFile(file, this) }
    }

    override fun findFileByNioFile(file: Path): VirtualFile? {
        return findFileByIoFile(file.toFile())
    }
}
