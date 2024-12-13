package org.jetbrains.kotlin.analysis.api.dumdum.index

import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy

class ByteArrayKey(
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other === this || (other is ByteArrayKey && other.bytes contentEquals bytes)

    override fun hashCode(): Int =
        bytes.contentHashCode()
}

data class ValueKey<T>(
    val fileId: FileId,
    val valueType: ValueType<T>,
)

fun inMemoryIndex(updates: List<IndexUpdate<*>>): Index {
    val fileToValue: Map<ValueKey<*>, List<ByteArray>> =
        updates.groupBy(
            keySelector = { indexUpdate ->
                ValueKey(indexUpdate.fileId, indexUpdate.valueType)
            },
            valueTransform = { indexUpdate ->
                indexUpdate.serializeValue()
            }
        )

    val keyToFiles: Map<ByteArrayKey, List<FileId>> =
        updates.flatGroupBy(
            keySelector = { it.keys },
            keyTransformer = { key ->
                ByteArrayKey(key.serialize())
            },
            valueTransformer = { it.fileId }
        )

    val keyTypeToKeys: Map<KeyType<*>, List<ByteArray>> =
        updates
            .flatMap { indexUpdate ->
                indexUpdate.keys.map { key ->
                    key.keyType to key.serialize()
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )

    return object : Index {

        override fun <S> value(fileId: FileId, valueType: ValueType<S>): S? =
            fileToValue[ValueKey(fileId, valueType)]
                ?.firstOrNull()
                ?.let { bb ->
                    valueType.serializer.deserialize(bb)
                }

        override fun <K> files(key: IndexKey<K>): Sequence<FileId> =
            keyToFiles[ByteArrayKey(key.serialize())]
                ?.asSequence()
                .orEmpty()

        override fun <K> keys(keyType: KeyType<K>): Sequence<K> =
            keyTypeToKeys[keyType]
                ?.asSequence()
                ?.map { key ->
                    keyType.serializer.deserialize(key)
                }
                .orEmpty()
    }
}