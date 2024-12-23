package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor

data class FileBasedIndexExtensions(
    val keyTypesMap: KeyTypesMap,
    val mapTypes: Map<ID<*, *>, ValueType<FileBasedMap<*, *>>>,
    val extensions: List<FileBasedIndexExtension<*, *>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <K, V> mapType(id: ID<K, V>): ValueType<FileBasedMap<K, V>> =
        mapTypes[id as ID<Any, Any?>]!! as ValueType<FileBasedMap<K, V>>
}

data class FileBasedMap<K, V>(
    val map: Map<K, Box<V>>,
) {
    companion object {
        fun <K, V> serializer(
            keyExternalizer: DataExternalizer<K>,
            valueExternalizer: DataExternalizer<V>,
        ): Serializer<FileBasedMap<K, V>> {
            val mapSerializer = MapExternalizer(
                keyExternalizer = keyExternalizer,
                valueExternalizer = Box.externalizer(valueExternalizer)
            ).asSerializer()
            return object : Serializer<FileBasedMap<K, V>> {
                override fun serialize(value: FileBasedMap<K, V>): ByteArray =
                    mapSerializer.serialize(value.map)

                override fun deserialize(bytes: ByteArray): FileBasedMap<K, V> =
                    FileBasedMap(mapSerializer.deserialize(bytes))
            }
        }
    }
}

fun fileBasedIndexExtensions(fileBasedIndexExtensions: List<FileBasedIndexExtension<*, *>>): FileBasedIndexExtensions {
    val keyTypesMap = keyTypesMap(
        fileBasedIndexExtensions.map { extension ->
            @Suppress("UNCHECKED_CAST")
            extension.name to (extension.keyDescriptor as KeyDescriptor<Any?>)
        })

    return FileBasedIndexExtensions(
        keyTypesMap = keyTypesMap,
        extensions = fileBasedIndexExtensions,
        mapTypes = fileBasedIndexExtensions.associate { extension ->
            val keyType = keyTypesMap.keyType(extension.name)
            @Suppress("UNCHECKED_CAST")
            extension.name to ValueType(
                keys = setOf(keyType),
                id = extension.name.name,
                serializer = FileBasedMap.serializer(
                    extension.keyDescriptor,
                    extension.valueExternalizer
                ) as Serializer<FileBasedMap<*, *>>,
                valueIndexer = ValueIndexer { map ->
                    ValueIndex(mapOf(keyType to map.map.keys))
                }
            )
        }
    )
}
