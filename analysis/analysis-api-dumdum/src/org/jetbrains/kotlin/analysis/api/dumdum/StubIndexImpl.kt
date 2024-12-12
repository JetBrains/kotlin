package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.*
import com.intellij.util.Processor
import com.intellij.util.containers.HashingStrategy
import com.intellij.util.indexing.ID
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
                                            val idCount = dis.readInt()
                                            val ids = IntArray(idCount) {
                                                val id = dis.readInt()
                                                id
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

fun interface FilePathExtractor {
    fun filePath(virtualFile: VirtualFile): FileId
}

fun stubIndexExtensions(
    stubIndexExtensions: List<StubIndexExtension<*, *>>,
    stubSerializersTable: StubSerializersTable,
): StubIndexExtensions {
    val keyTypesMap = keyTypesMap(
        stubIndexExtensions.map { extension ->
            @Suppress("UNCHECKED_CAST")
            extension.key to extension.keyDescriptor as KeyDescriptor<Any?>
        }
    )
    return StubIndexExtensions(
        keyTypesMap = keyTypesMap,
        stubValueType = ValueType(
            id = "stub",
            serializer = StubValue.serializer(
                keyTypesMap = keyTypesMap,
                stubSerializersTable = stubSerializersTable
            )
        ),
    )
}

data class StubIndexExtensions(
    val keyTypesMap: KeyTypesMap,
    val stubValueType: ValueType<StubValue>,
)

fun Index.stubIndex(
    stubIndexExtensions: StubIndexExtensions,
    virtualFileFactory: VirtualFileFactory,
    documentIdMapper: FilePathExtractor,
): StubIndex = let { index ->
    object : StubIndex {
        override fun stub(virtualFile: VirtualFile): ObjectStubTree<*>? =
            index.value(
                fileId = documentIdMapper.filePath(virtualFile),
                valueType = stubIndexExtensions.stubValueType
            )?.stub

        override fun <K> getContainingFilesIterator(
            indexId: ID<K, *>,
            dataKey: K,
            project: Project,
            scope: GlobalSearchScope,
        ): Iterator<VirtualFile> =
            index
                .files(
                    IndexKey(
                        keyType = stubIndexExtensions.keyTypesMap.keyType(indexId),
                        key = dataKey
                    )
                )
                .map(virtualFileFactory::virtualFile)
                .filter { scope.contains(it) }
                .iterator()

        override fun <Key, Psi : PsiElement> processElements(
            indexKey: StubIndexKey<Key, Psi>,
            key: Key,
            project: Project,
            scope: GlobalSearchScope,
            requiredClass: Class<Psi>,
            processor: Processor<in Psi>,
        ): Boolean =
            index
                .files(
                    IndexKey(
                        keyType = stubIndexExtensions.keyTypesMap.keyType(indexKey),
                        key = key
                    )
                )
                .filter { scope.contains(virtualFileFactory.virtualFile(it)) }
                .mapNotNull { index.value(it, stubIndexExtensions.stubValueType) }
                .flatMap { stubValue ->
                    stubValue.index[indexKey]?.get(key as Any)?.map { stubId ->
                        @Suppress("UNCHECKED_CAST")
                        stubValue.stub.plainList[stubId].psi as Psi
                    }?.asSequence() ?: emptySequence()
                }
                .all(processor::process)
    }
}

fun stubIndexesUpdate(
    fileId: FileId,
    tree: StubTree,
    stubIndexExtensions: StubIndexExtensions,
): IndexUpdate<*> {
    val map = tree.indexStubTree { indexKey ->
        HashingStrategy.canonical()
    }
    return IndexUpdate(
        fileId = fileId,
        valueType = stubIndexExtensions.stubValueType,
        value = StubValue(tree, map),
        keys = map.flatMap { (stubIndexKey, stubIndex) ->
            @Suppress("UNCHECKED_CAST")
            val keyType = stubIndexExtensions.keyTypesMap.keyType(stubIndexKey) as KeyType<Any>
            stubIndex.keys.map { key ->
                IndexKey(keyType, key)
            }
        }
    )
}
