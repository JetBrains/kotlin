package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.ShareableStubTreeSerializer
import com.intellij.psi.stubs.StubTree
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import java.io.DataInput
import java.io.DataOutput

data class SerializedStubTree(val bytes: ByteArray) {
    companion object {
        fun deserialize(dataInput: DataInput): SerializedStubTree {
            val size = dataInput.readInt()
            val bytes = ByteArray(size)
            dataInput.readFully(bytes)
            return SerializedStubTree(bytes)
        }
    }

    fun deserialize(psiFile: PsiFile, stubSerializersTable: StubSerializersTable): StubTree {
        val stubSerializer = ShareableStubTreeSerializer(stubSerializersTable)
        val bais = UnsyncByteArrayInputStream(bytes)
        val stub = bais.use {
            stubSerializer.deserialize(bais)
        }
        @Suppress("UNCHECKED_CAST")
        (stub as PsiFileStubImpl<PsiFile>).psi = psiFile
        return StubTree(stub)
    }

    override fun equals(other: Any?): Boolean =
        other is SerializedStubTree && other.bytes.contentEquals(bytes)

    override fun hashCode(): Int =
        bytes.contentHashCode()

    fun serialize(dataOutput: DataOutput) {
        dataOutput.writeInt(bytes.size)
        dataOutput.write(bytes)
    }
}

fun StubTree.serialize(stubSerializersTable: StubSerializersTable): SerializedStubTree {
    val stubSerializer = ShareableStubTreeSerializer(stubSerializersTable)
    val baos = UnsyncByteArrayOutputStream()
    baos.use { os ->
        stubSerializer.serialize(root, os)
    }
    return SerializedStubTree(baos.toByteArray())
}
