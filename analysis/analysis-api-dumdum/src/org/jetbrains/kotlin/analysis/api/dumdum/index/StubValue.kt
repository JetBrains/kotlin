package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class StubValue(
    val stub: StubTree,
    val index: Map<StubIndexKey<*, *>, Map<Any?, IntArray>>,
) {
    companion object {
        fun serializer(
            keyTypesMap: KeyTypesMap,
            stubSerializersTable: StubSerializersTable,
        ): Serializer<StubValue> {
            val stubSerializer = ShareableStubTreeSerializer(stubSerializersTable)
            return object : Serializer<StubValue> {
                override fun serialize(value: StubValue): ByteArray {
                    val baos = UnsyncByteArrayOutputStream()
                    baos.use { os ->
                        stubSerializer.serialize(value.stub.root, os)
                        DataOutputStream(os).use { dos ->
                            val indexesCount = value.index.size
                            dos.writeInt(indexesCount)
                            for ((indexId, map) in value.index) {
                                dos.writeUTF(indexId.name)
                                dos.writeInt(map.size)

                                @Suppress("UNCHECKED_CAST")
                                val keyDescriptor = keyTypesMap.keyDescriptor(indexId) as KeyDescriptor<Any?>
                                for ((k, ids) in map) {
                                    keyDescriptor.save(dos, k)
                                    dos.writeInt(ids.size)
                                    for (id in ids) {
                                        dos.writeInt(id)
                                    }
                                }
                            }
                        }
                    }
                    return baos.toByteArray()
                }

                override fun deserialize(bytes: ByteArray): StubValue =
                    UnsyncByteArrayInputStream(bytes).use { i ->
                        val root = stubSerializer.deserialize(i)
                        val tree = StubTree(root as PsiFileStub<*>)
                        val index = DataInputStream(i).use { dis ->
                            val indexesCount = dis.readInt()
                            buildMap(indexesCount) {
                                repeat(indexesCount) {
                                    val indexName = dis.readUTF()
                                    val indexId = StubIndexKey.createIndexKey<Any?, PsiElement>(indexName) as StubIndexKey<*, *>

                                    @Suppress("UNCHECKED_CAST")
                                    val keyDescriptor = keyTypesMap.keyDescriptor(indexId) as KeyDescriptor<Any?>
                                    val keysCount = dis.readInt()
                                    val index = buildMap(keysCount) {
                                        repeat(keysCount) {
                                            val key = keyDescriptor.read(dis)
                                            val idSize = dis.readInt()
                                            val ids = IntArray(idSize) {
                                                dis.readInt()
                                            }
                                            put(key, ids)
                                        }
                                    }
                                    put(indexId, index)
                                }
                            }
                        }
                        StubValue(tree, index)
                    }
            }
        }
    }
}