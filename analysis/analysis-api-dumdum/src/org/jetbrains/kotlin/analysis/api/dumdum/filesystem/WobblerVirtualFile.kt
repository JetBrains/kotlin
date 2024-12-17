package org.jetbrains.kotlin.analysis.api.dumdum.filesystem

import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.testFramework.LightVirtualFileBase
import com.intellij.util.io.UnsyncByteArrayInputStream
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileId
import java.io.InputStream
import java.io.OutputStream

fun interface FileReader {
    fun read(fileId: FileId): ByteArray
}

class WobblerVirtualFile(
    val fileReader: FileReader,
    val fileId: FileId,
    fileType: FileType,
) : LightVirtualFileBase(fileId.id, fileType, 0L) { 
    init {
        SingleRootFileViewProvider.doNotCheckFileSizeLimit(this)
    }
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("not writable")
    }

    override fun contentsToByteArray(): ByteArray =
        fileReader.read(fileId)

    override fun getInputStream(): InputStream =
        UnsyncByteArrayInputStream(contentsToByteArray())

    override fun equals(other: Any?): Boolean =
        other is WobblerVirtualFile && other.fileId == fileId

    override fun hashCode(): Int =
        fileId.hashCode() + 3
} 