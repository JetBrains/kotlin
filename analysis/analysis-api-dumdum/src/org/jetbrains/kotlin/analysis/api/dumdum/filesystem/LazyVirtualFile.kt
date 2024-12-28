package org.jetbrains.kotlin.analysis.api.dumdum.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.util.io.UnsyncByteArrayInputStream
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileId
import java.io.InputStream
import java.io.OutputStream

class LazyVirtualFile(
    val vfs: LazyVirtualFileSystem,
    val fileId: FileId,
    val fileReader: () -> ByteArray,
) : VirtualFile(), VirtualFileWithId {

    val bytes = lazy { fileReader() }

    init {
        SingleRootFileViewProvider.doNotCheckFileSizeLimit(this)
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("not writable")
    }

    override fun contentsToByteArray(): ByteArray =
        bytes.value

    override fun getTimeStamp(): Long =
        0L

    override fun getLength(): Long =
        bytes.value.size.toLong()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {

    }

    override fun getInputStream(): InputStream =
        UnsyncByteArrayInputStream(contentsToByteArray())

    override fun equals(other: Any?): Boolean =
        other is LazyVirtualFile && other.fileId == fileId

    override fun hashCode(): Int =
        fileId.hashCode() + 3

    override fun getId(): Int {
        throw UnsupportedOperationException("no id here")
    }

    override fun getName(): String =
        fileId.fileName

    override fun getFileSystem(): VirtualFileSystem =
        vfs

    override fun getPath(): String = 
        fileId.fileName

    override fun isWritable(): Boolean =
        false

    override fun isDirectory(): Boolean =
        false

    override fun isValid(): Boolean =
        true

    override fun getParent(): VirtualFile? =
        null

    override fun getChildren(): Array<VirtualFile> =
        emptyArray()

    override fun toString(): String =
        "LazyVirtualFile($fileId)"
} 