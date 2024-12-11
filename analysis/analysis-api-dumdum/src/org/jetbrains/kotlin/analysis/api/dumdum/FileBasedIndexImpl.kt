package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.KeyDescriptor

data class Box<T>(val value: T)

fun interface VirtualFileFactory {
    fun virtualFile(fileId: FileId): VirtualFile
}

data class KeyTypesMap(private val map: Map<ID<*, *>, KeyType<Any?>>) {
    @Suppress("UNCHECKED_CAST")
    fun <K> keyType(indexId: ID<K, *>): KeyType<K> =
        requireNotNull(map[indexId]) {
            "keyType is not found for indexId $indexId"
        } as KeyType<K>
}

fun Index.fileBased(
    virtualFileFactory: VirtualFileFactory,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
): FileBasedIndex = let { index ->
    object : FileBasedIndex {
        override fun <K, V> processValues(
            indexId: ID<K, V>,
            dataKey: K,
            filter: GlobalSearchScope,
            processor: Processor<in V>,
        ): Boolean {
            val valueDescriptor = fileBasedIndexExtensions.mapType(indexId)
            val keyType = fileBasedIndexExtensions.keyTypesMap.keyType(indexId)
            return index
                .files(IndexKey(keyType, dataKey))
                .filter { filter.contains(virtualFileFactory.virtualFile(it)) }
                .mapNotNull { documentId ->
                    index
                        .value(documentId, valueDescriptor)
                        ?.get(dataKey)
                }
                .all { (v) -> processor.process(v) }
        }

        override fun <K, V> processAllKeys(
            indexId: ID<K, V>,
            filter: GlobalSearchScope,
            processor: Processor<in K>,
        ): Boolean {
            val keyType = fileBasedIndexExtensions.keyTypesMap.keyType(indexId)
            return index
                .keys(keyType)
                .filter { k ->
                    index
                        .files(IndexKey(keyType, k))
                        .any { filter.contains(virtualFileFactory.virtualFile(it)) }
                }
                .all(processor::process)
        }
    }
}

data class FileBasedIndexExtensions(
    val keyTypesMap: KeyTypesMap,
    val mapTypes: Map<ID<Any, Any?>, ValueType<Map<Any, Box<Any?>>>>,
    val extensions: List<FileBasedIndexExtension<*, *>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <K, V> mapType(id: ID<K, V>): ValueType<Map<K, Box<V>>> =
        mapTypes[id as ID<Any, Any?>]!! as ValueType<Map<K, Box<V>>>
}

fun fileBasedIndexExtensions(fileBasedIndexExtensions: List<FileBasedIndexExtension<*, *>>): FileBasedIndexExtensions =
    FileBasedIndexExtensions(
        keyTypesMap = KeyTypesMap(
            fileBasedIndexExtensions.associate { extension ->
                @Suppress("UNCHECKED_CAST")
                extension.name to KeyType(
                    id = extension.name.name,
                    serializer = (extension.keyDescriptor as KeyDescriptor<Any>).asSerializer()
                )
            }),
        extensions = fileBasedIndexExtensions,
        mapTypes = fileBasedIndexExtensions.associate { extension ->
            @Suppress("UNCHECKED_CAST")
            extension as FileBasedIndexExtension<Any, Any?>
            extension.name to ValueType(
                extension.name.name,
                MapSerializer(
                    extension.keyDescriptor.asSerializer(),
                    BoxSerializer(extension.valueExternalizer.asSerializer())
                )
            )
        }
    )

class MapSerializer<K, V>(
    keySerializer: Serializer<K>,
    valueSerializer: Serializer<V>,
) : Serializer<Map<K, V>> {
    override fun serialize(t: Map<K, V>): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(bytes: ByteArray): Map<K, V> {
        TODO("Not yet implemented")
    }

}

class BoxSerializer<V>(private val serializer: Serializer<V>) : Serializer<Box<V>> {
    override fun serialize(t: Box<V>): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(bytes: ByteArray): Box<V> {
        TODO("Not yet implemented")
    }
}

fun fileBasedIndexesUpdates(
    fileId: FileId,
    fileContent: FileContent,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
): List<IndexUpdate<*>> =
    fileBasedIndexExtensions.extensions.map { extension ->
        @Suppress("UNCHECKED_CAST")
        extension as FileBasedIndexExtension<Any, Any?>
        val indexId = extension.name
        val map = extension.indexer.map(fileContent).mapValues { (_, v) -> Box(v) }
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
