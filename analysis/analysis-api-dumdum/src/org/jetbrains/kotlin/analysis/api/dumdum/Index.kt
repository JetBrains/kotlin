package org.jetbrains.kotlin.analysis.api.dumdum

interface Index {
    fun <S> value(fileId: FileId, valueType: ValueType<S>): S?

    fun <K> files(key: IndexKey<K>): Sequence<FileId>

    fun <K> keys(keyType: KeyType<K>): Sequence<K>
}

@JvmInline
value class FileId(val id: String)

data class IndexKey<K>(
    val keyType: KeyType<K>,
    val key: K,
)

class KeyType<K>(
    val id: String,
    val serializer: Serializer<K>,
) {
    override fun equals(other: Any?): Boolean =
        other === this || (other is KeyType<*> && other.id == this.id)

    override fun hashCode(): Int =
        id.hashCode() + 1
}

class ValueType<S>(
    val id: String,
    val serializer: Serializer<S>,
) {
    override fun equals(other: Any?): Boolean =
        other === this || (other is ValueType<*> && other.id == this.id)

    override fun hashCode(): Int =
        id.hashCode() + 2
}

interface Serializer<T> {

    fun serialize(t: T): ByteArray

    fun deserialize(bytes: ByteArray): T

    companion object {

        private val DUMMY = object : Serializer<Any> {
            override fun serialize(t: Any): ByteArray {
                throw UnsupportedOperationException()
            }

            override fun deserialize(bytes: ByteArray): Any {
                throw UnsupportedOperationException()
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> dummy(): Serializer<T> = DUMMY as Serializer<T>
    }
}

data class IndexUpdate<T>(
    val fileId: FileId,
    val valueType: ValueType<T>,
    val value: T,
    val keys: List<IndexKey<*>>,
)
