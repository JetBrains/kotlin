package org.jetbrains.kotlin.analysis.api.dumdum.filesystem

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.VirtualFile

class LazyVirtualFileSystem : DeprecatedVirtualFileSystem() {
    override fun getProtocol(): String =
        "wobbler"

    override fun findFileByPath(path: String): VirtualFile? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun refresh(asynchronous: Boolean) {

    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        throw UnsupportedOperationException("not implemented")
    }

}