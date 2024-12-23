package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.indexing.ID
import com.intellij.util.io.KeyDescriptor

data class KeyTypesMap(
    private val idToKeyDescriptor: Map<ID<*, *>, KeyDescriptor<*>>,
    private val idToKeyType: Map<ID<*, *>, KeyType<*>>,
) {
    val keyTypes: List<KeyType<*>> = idToKeyType.values.toList()
    
    @Suppress("UNCHECKED_CAST")
    fun <K> keyDescriptor(indexId: ID<K, *>): KeyDescriptor<K> =
        requireNotNull(idToKeyDescriptor[indexId]) {
            "keyType is not found for indexId $indexId"
        } as KeyDescriptor<K>

    @Suppress("UNCHECKED_CAST")
    fun <K> keyType(indexId: ID<K, *>): KeyType<K> =
        requireNotNull(idToKeyType[indexId]) {
            "keyType is not found for indexId $indexId"
        } as KeyType<K>
}

fun keyTypesMap(keys: List<Pair<ID<*, *>, KeyDescriptor<*>>>): KeyTypesMap =
    KeyTypesMap(
        idToKeyDescriptor = keys.toMap(),
        idToKeyType = keys.associate { (indexId, keyDescriptor) ->
            indexId to KeyType(indexId.name, keyDescriptor.asSerializer())
        }
    )
