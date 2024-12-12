package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.indexing.ID
import com.intellij.util.io.KeyDescriptor

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
