@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin.collections

import kotlin.sequences.*

private object EmptyMap : Map<Any?, Nothing>, Serializable {
    private const val serialVersionUID: Long = 8246714829545688274

    override fun equals(other: Any?): Boolean = other is Map<*, *> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "{}"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true

    override fun containsKey(key: Any?): Boolean = false
    override fun containsValue(value: Nothing): Boolean = false
    override fun get(key: Any?): Nothing? = null
    override val entries: Set<Map.Entry<Any?, Nothing>> get() = EmptySet
    override val keys: Set<Any?> get() = EmptySet
    override val values: Collection<Nothing> get() = EmptyList

    private fun readResolve(): Any = EmptyMap
}

public fun <K, V> emptyMap(): Map<K, V> = @Suppress("UNCHECKED_CAST") (EmptyMap as Map<K, V>)

public fun <K, V> mapOf(vararg pairs: Pair<K, V>): Map<K, V> =
    if (pairs.size > 0) pairs.toMap(LinkedHashMap(mapCapacity(pairs.size))) else emptyMap()

public inline fun <K, V> mapOf(): Map<K, V> = emptyMap()

public inline fun <K, V> mutableMapOf(): MutableMap<K, V> = LinkedHashMap()

public fun <K, V> mutableMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V> =
    LinkedHashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

public inline fun <K, V> hashMapOf(): HashMap<K, V> =  kotlin.UnsupportedOperationException("This is intrinsic")

public fun <K, V> hashMapOf(vararg pairs: Pair<K, V>): HashMap<K, V> = kotlin.UnsupportedOperationException("This is intrinsic")

public inline fun <K, V> linkedMapOf(): LinkedHashMap<K, V> = LinkedHashMap<K, V>()

internal fun mapCapacity(expectedSize: Int): Int = when {
    // We are not coercing the value to a valid one and not throwing an exception. It is up to the caller to
    // properly handle negative values.
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    expectedSize < 1 shl (Int.SIZE_BITS - 2) -> ((expectedSize / 0.75F) + 1.0F).toInt()
    // any large value
    else -> Int.MAX_VALUE
}

public inline operator fun <K, V> Map<out K, V>.contains(key: K): Boolean = containsKey(key)

public inline operator fun <K, V> Map<out K, V>.get(key: K): V? = (this as Map<K, V>).get(key)

public inline operator fun <K, V> MutableMap<K, V>.set(key: K, value: V): Unit {
    put(key, value)
}

public inline fun <K> Map<out K, *>.containsKey(key: K): Boolean = (this as Map<K, *>).containsKey(key)

public inline fun <K, V> Map<K, V>.containsValue(value: V): Boolean = this.containsValue(value)

public inline fun <K, V> MutableMap<out K, V>.remove(key: K): V? = (this as MutableMap<K, V>).remove(key)

public inline operator fun <K, V> Map.Entry<K, V>.component1(): K = key

public inline operator fun <K, V> Map.Entry<K, V>.component2(): V = value

public inline fun <K, V> Map.Entry<K, V>.toPair(): Pair<K, V> = Pair(key, value)

public inline operator fun <K, V> Map<out K, V>.iterator(): Iterator<Map.Entry<K, V>> = entries.iterator()

public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Array<out Pair<K, V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Iterable<Pair<K, V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Sequence<Pair<K, V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> {
    if (this is Collection) {
        return when (size) {
            0 -> emptyMap()
            1 -> mapOf(if (this is List) this[0] else iterator().next())
            else -> toMap(LinkedHashMap<K, V>(mapCapacity(size)))
        }
    }
    return toMap(LinkedHashMap<K, V>()).optimizeReadOnlyMap()
}

public fun <K, V, M : MutableMap<in K, in V>> Iterable<Pair<K, V>>.toMap(destination: M): M = destination.apply { putAll(this@toMap) }

public fun <K, V> Array<out Pair<K, V>>.toMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> mapOf(this[0])
    else -> toMap(LinkedHashMap<K, V>(mapCapacity(size)))
}

public fun <K, V, M : MutableMap<in K, in V>> Array<out Pair<K, V>>.toMap(destination: M): M = destination.apply { putAll(this@toMap) }

public fun <K, V> Sequence<Pair<K, V>>.toMap(): Map<K, V> = toMap(LinkedHashMap<K, V>()).optimizeReadOnlyMap()

public fun <K, V, M : MutableMap<in K, in V>> Sequence<Pair<K, V>>.toMap(destination: M): M = destination.apply { putAll(this@toMap) }

internal fun <K, V> Map<K, V>.optimizeReadOnlyMap() = when (size) {
    0 -> emptyMap()
    1 -> this // toSingletonMapOrSelf()
    else -> this
}
