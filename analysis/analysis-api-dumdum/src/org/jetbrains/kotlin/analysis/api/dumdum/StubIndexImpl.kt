package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.stubs.StubTree
import com.intellij.util.Processor
import com.intellij.util.containers.HashingStrategy
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor

data class StubValue(
    val stub: StubTree,
    val index: Map<StubIndexKey<*, *>, Map<Any, IntArray>>,
)

val StubIndexValueType: ValueType<StubValue> =
    ValueType("psi.stub", Serializer.dummy())

fun interface FilePathExtractor {
    fun filePath(virtualFile: VirtualFile): FileId
}

fun stubIndexExtensions(stubIndexExtensions: List<StubIndexExtension<*, *>>): KeyTypesMap =
    KeyTypesMap(stubIndexExtensions.associate { extension ->
        @Suppress("UNCHECKED_CAST")
        extension.key to KeyType(
            id = extension.key.name,
            serializer = (extension.keyDescriptor as KeyDescriptor<Any?>).asSerializer()
        )
    })

fun Index.stubIndex(
    stubIndexExtensions: KeyTypesMap,
    virtualFileFactory: VirtualFileFactory,
    documentIdMapper: FilePathExtractor,
): StubIndex = let { index ->
    object : StubIndex {
        override fun stub(virtualFile: VirtualFile): ObjectStubTree<*>? =
            index.value(
                fileId = documentIdMapper.filePath(virtualFile),
                valueType = StubIndexValueType
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
                        keyType = stubIndexExtensions.keyType(indexId),
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
                        keyType = stubIndexExtensions.keyType(indexKey),
                        key = key
                    )
                )
                .filter { scope.contains(virtualFileFactory.virtualFile(it)) }
                .mapNotNull { index.value(it, StubIndexValueType) }
                .flatMap { stubValue ->
                    stubValue.index[indexKey]?.get(key as Any)?.map { stubId ->
                        @Suppress("UNCHECKED_CAST")
                        stubValue.stub.plainList[stubId].psi as Psi
                    }?.asSequence() ?: emptySequence()
                }
                .all(processor::process)
    }
}

fun <T> DataExternalizer<T>.asSerializer(): Serializer<T> =
    object : Serializer<T> {
        override fun serialize(t: T): ByteArray {
            TODO("Not yet implemented")
        }

        override fun deserialize(bytes: ByteArray): T {
            TODO("Not yet implemented")
        }
    }

fun stubIndexesUpdate(
    fileId: FileId,
    tree: StubTree,
    stubIndexExtensions: KeyTypesMap,
): IndexUpdate<*> {
    val map = tree.indexStubTree { indexKey ->
        HashingStrategy.canonical()
    }
    return IndexUpdate(
        fileId = fileId,
        valueType = StubIndexValueType,
        value = StubValue(tree, map),
        keys = map.flatMap { (stubIndexKey, stubIndex) ->
            @Suppress("UNCHECKED_CAST")
            val keyType = stubIndexExtensions.keyType(stubIndexKey) as KeyType<Any>
            stubIndex.keys.map { key ->
                IndexKey(keyType, key)
            }
        }
    )
}
