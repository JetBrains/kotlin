package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.indexing.ID
import com.intellij.util.io.KeyDescriptor

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
                    keyExternalizer = extension.keyDescriptor,
                    valueExternalizer = Box.externalizer(extension.valueExternalizer)
                ).asSerializer()
            )
        }
    )
