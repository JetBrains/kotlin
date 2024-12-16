package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubTree
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.containers.HashingStrategy
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileContentImpl

data class IndexUpdate<T>(
    val fileId: FileId,
    val valueType: ValueType<T>,
    val value: T,
    val keys: List<IndexKey<*>>,
)

fun indexFile(
    fileId: FileId,
    file: PsiFile,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
    stubSerializerTable: StubSerializersTable,
    stubIndexExtensions: StubIndexExtensions,
): List<IndexUpdate<*>> =
    fileBasedIndexesUpdates(
        fileId = fileId,
        fileContent = FileContentImpl.createByFile(file.virtualFile, file.project),
        fileBasedIndexExtensions = fileBasedIndexExtensions
    ) +
        (file.fileElementType as? IStubFileElementType<*>)?.let { stubFileElementType ->
            val stubElement = stubFileElementType.builder.buildStubTree(file)
            listOf(
                stubIndexesUpdate(
                    stubIndexExtensions = stubIndexExtensions,
                    fileId = fileId,
                    tree = StubTree(stubElement as PsiFileStub<*>),
                    stubSerializerTable = stubSerializerTable,
                )
            )
        }.orEmpty()

private fun stubIndexesUpdate(
    fileId: FileId,
    tree: StubTree,
    stubSerializerTable: StubSerializersTable,
    stubIndexExtensions: StubIndexExtensions,
): IndexUpdate<*> {
    val map = tree.indexStubTree { indexKey ->
        HashingStrategy.canonical()
    }
    return IndexUpdate(
        fileId = fileId,
        valueType = stubIndexExtensions.indexedSerializedStubTreeType,
        value = IndexedSerializedStubTree(tree.serialize(stubSerializerTable), map),
        keys = map.flatMap { (stubIndexKey, stubIndex) ->
            @Suppress("UNCHECKED_CAST")
            val keyType = stubIndexExtensions.keyTypesMap.keyType(stubIndexKey) as KeyType<Any>
            stubIndex.keys.map { key ->
                IndexKey(keyType, key)
            }
        }
    )
}

private fun fileBasedIndexesUpdates(
    fileId: FileId,
    fileContent: FileContent,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
): List<IndexUpdate<*>> =
    fileBasedIndexExtensions.extensions.map { extension ->
        @Suppress("UNCHECKED_CAST")
        extension as FileBasedIndexExtension<Any, Any?>
        val indexId = extension.name
        val map = extension.indexer.map(fileContent).mapValues { (_, v) -> Box<Any?>(v) }
        val keyType = fileBasedIndexExtensions.keyTypesMap.keyType(indexId)
        IndexUpdate(
            fileId = fileId,
            valueType = fileBasedIndexExtensions.mapType(indexId),
            value = map,
            keys = map.keys.map { key ->
                IndexKey(keyType, key)
            }
        )
    }

fun <T> IndexUpdate<T>.serializeValue(): ByteArray =
    valueType.serializer.serialize(value)
