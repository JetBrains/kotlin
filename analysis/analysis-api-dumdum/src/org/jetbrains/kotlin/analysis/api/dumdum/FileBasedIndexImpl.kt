package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream

data class Box<T>(val value: T?)

fun interface VirtualFileFactory {
    fun virtualFile(fileId: FileId): VirtualFile
}

data class KeyTypesMap(
    private val keyDescriptors: Map<ID<*, *>, KeyDescriptor<*>>,
    private val keyTypes: Map<ID<*, *>, KeyType<*>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <K> keyDescriptor(indexId: ID<K, *>): KeyDescriptor<K> =
        requireNotNull(keyDescriptors[indexId]) {
            "keyType is not found for indexId $indexId"
        } as KeyDescriptor<K>

    @Suppress("UNCHECKED_CAST")
    fun <K> keyType(indexId: ID<K, *>): KeyType<K> =
        requireNotNull(keyTypes[indexId]) {
            "keyType is not found for indexId $indexId"
        } as KeyType<K>
}

fun keyTypesMap(keys: List<Pair<ID<*, *>, KeyDescriptor<*>>>): KeyTypesMap =
    KeyTypesMap(
        keyDescriptors = keys.toMap(),
        keyTypes = keys.associate { (indexId, keyDescriptor) ->
            indexId to KeyType(indexId.name, keyDescriptor.asSerializer())
        }
    )

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
        keyTypesMap = keyTypesMap(
            fileBasedIndexExtensions.map { extension ->
                @Suppress("UNCHECKED_CAST")
                extension.name to (extension.keyDescriptor as KeyDescriptor<Any?>)
            }),
        extensions = fileBasedIndexExtensions,
        mapTypes = fileBasedIndexExtensions.associate { extension ->
            @Suppress("UNCHECKED_CAST")
            extension as FileBasedIndexExtension<Any, Any?>
            extension.name to ValueType(
                id = extension.name.name,
                serializer = MapExternalizer(
                    extension.keyDescriptor,
                    BoxExternalizer(extension.valueExternalizer)
                ).asSerializer()
            )
        }
    )

fun <T> DataExternalizer<T>.asSerializer(): Serializer<T> = let { externalizer ->
    object : Serializer<T> {
        override fun serialize(value: T): ByteArray {
            val baos = UnsyncByteArrayOutputStream()
            baos.use { os ->
                DataOutputStream(os).use { dos ->
                    externalizer.save(dos, value)
                }
            }
            return baos.toByteArray()
        }

        override fun deserialize(bytes: ByteArray): T =
            UnsyncByteArrayInputStream(bytes).use { i ->
                DataInputStream(i).use { dis ->
                    externalizer.read(dis)
                }
            }
    }
}

class MapExternalizer<K, V>(
    val keySerializer: DataExternalizer<K>,
    val valueSerializer: DataExternalizer<V>,
) : DataExternalizer<Map<K, V>> {
    override fun save(out: DataOutput, value: Map<K, V>) {
        out.writeInt(value.size)
        for ((k, v) in value) {
            keySerializer.save(out, k)
            valueSerializer.save(out, v)
        }
    }

    override fun read(`in`: DataInput): Map<K, V> {
        val size = `in`.readInt()
        return buildMap(size) {
            repeat(size) {
                val k = keySerializer.read(`in`)
                val v = valueSerializer.read(`in`)
                put(k, v)
            }
        }
    }
}

class BoxExternalizer<V>(private val serializer: DataExternalizer<V>) : DataExternalizer<Box<V>> {
    override fun save(out: DataOutput, value: Box<V>) {
        out.writeBoolean(value.value != null)
        value.value?.let { x ->
            serializer.save(out, x)
        }
    }

    override fun read(`in`: DataInput): Box<V> {
        val some = `in`.readBoolean()
        return Box(if (some) serializer.read(`in`) else null)
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
