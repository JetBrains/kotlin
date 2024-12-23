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

data class TypedKey<K>(
    val keyType: KeyType<K>,
    val bytes: ByteArrayKey,
)

fun inMemoryIndex(files: Map<FileId, FileValues>): Index {
    val fileToValue: Map<ValueKey<*>, ByteArray> =
        files.flatMap { (fileId, fileValues) ->
            fileValues.map.map { (valueType, value) ->
                @Suppress("UNCHECKED_CAST")
                valueType as ValueType<Any?>
                ValueKey(fileId, valueType) to valueType.serializer.serialize(value)
            }
        }.toMap()

    val keyToFiles: Map<TypedKey<*>, List<FileId>> =
        files.flatMap { (fileId, fileValues) ->
            fileValues.map.flatMap { (valueType, value) ->
                @Suppress("UNCHECKED_CAST")
                valueType as ValueType<Any?>
                valueType.valueIndexer.indexValue(value).map.flatMap { (keyType, keys) ->
                    @Suppress("UNCHECKED_CAST")
                    keyType as KeyType<Any?>
                    keys.map { key ->
                        TypedKey(keyType, ByteArrayKey(keyType.serializer.serialize(key))) to fileId
                    }
                }
            }
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second },
        )

    val keyTypeToKeys: Map<KeyType<*>, List<ByteArray>> =
        keyToFiles.keys.groupBy(
            keySelector = { it.keyType },
            valueTransform = { it.bytes.bytes }
        )


    return object : Index {

        override fun <S> value(fileId: FileId, valueType: ValueType<S>): S? =
            fileToValue[ValueKey(fileId, valueType)]
                ?.let { bb ->
                    valueType.serializer.deserialize(bb)
                }

        override fun <K> files(keyType: KeyType<K>, key: K): Sequence<FileId> =
            keyToFiles[TypedKey(keyType, ByteArrayKey(keyType.serializer.serialize(key)))]
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