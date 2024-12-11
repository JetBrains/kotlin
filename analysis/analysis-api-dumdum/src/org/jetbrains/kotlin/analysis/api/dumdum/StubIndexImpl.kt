package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.*
import com.intellij.util.Processor
import com.intellij.util.containers.HashingStrategy
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

data class StubValue(
    val stub: StubTree,
    val index: Map<StubIndexKey<*, *>, Map<Any, IntArray>>,
)

fun interface FilePathExtractor {
    fun filePath(virtualFile: VirtualFile): FileId
}

fun stubIndexExtensions(stubIndexExtensions: List<StubIndexExtension<*, *>>): StubIndexExtensions {
    val stubSerializersTable = StubSerializersTable.build()
    val stubSerializer = ShareableStubTreeSerializer(stubSerializersTable)
    return StubIndexExtensions(
        keyTypesMap = KeyTypesMap(
            stubIndexExtensions.associate { extension ->
                @Suppress("UNCHECKED_CAST")
                extension.key to KeyType(
                    id = extension.key.name,
                    serializer = (extension.keyDescriptor as KeyDescriptor<Any?>)
                )
            }
        ),
        stubValueType = ValueType(
            id = "stub",
            serializer = object : DataExternalizer<StubValue> {
                override fun save(out: DataOutput, value: StubValue) {
                    TODO("Not yet implemented")
                }

                override fun read(`in`: DataInput): StubValue {
                    TODO("Not yet implemented")
                }
            }
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
