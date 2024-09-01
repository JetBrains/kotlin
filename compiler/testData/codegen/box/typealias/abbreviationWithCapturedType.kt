// WITH_STDLIB

interface DynamoColumnType<V>
interface DynamoKeyColumnType<V> : DynamoColumnType<V>

open class DynamoColumn<V, T : DynamoColumnType<V>>
typealias DKeyColumn<V> = DynamoColumn<V, out DynamoKeyColumnType<V>>

sealed class DynamoKey

data class DynamoPartitionKey<P>(
    val partitionKey: DKeyColumn<P>
) : DynamoKey()

data class DynamoCompositeKey<P, S>(
    val partitionKey: DKeyColumn<P>,
    val sortKey: DKeyColumn<S>
) : DynamoKey()

val DynamoKey.columns
    get() = when (this) {
        is DynamoPartitionKey<*> -> listOf(partitionKey)
        is DynamoCompositeKey<*, *> -> listOf(partitionKey, sortKey)
    }

val DynamoKey.columnsSet
    get() = columns.toMutableSet()

fun box() = "OK"
