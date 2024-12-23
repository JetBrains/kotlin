package org.jetbrains.kotlin.analysis.api.dumdum.index

interface Index {
    fun <S> value(fileId: FileId, valueType: ValueType<S>): S?

    fun <K> files(keyType: KeyType<K>, key: K): Sequence<FileId>

    fun <K> keys(keyType: KeyType<K>): Sequence<K>
}

@JvmInline
value class FileId(val id: String)

interface Serializer<T> {
    fun serialize(value: T): ByteArray
    fun deserialize(bytes: ByteArray): T
}

class KeyType<K>(
    val id: String,
    val serializer: Serializer<K>,
) {
    override fun equals(other: Any?): Boolean =
        other === this || (other is KeyType<*> && other.id == this.id)

    override fun hashCode(): Int =
        id.hashCode() + 1
}

data class ValueIndex(val map: Map<KeyType<*>, Set<Any?>>)

fun interface ValueIndexer<V> {
    fun indexValue(value: V): ValueIndex
}

class ValueType<S>(
    val id: String,
    val serializer: Serializer<S>,
    val keys: Set<KeyType<*>>,
    val valueIndexer: ValueIndexer<S>,
) {
    override fun equals(other: Any?): Boolean =
        other === this || (other is ValueType<*> && other.id == this.id)

    override fun hashCode(): Int =
        id.hashCode() + 2
}
