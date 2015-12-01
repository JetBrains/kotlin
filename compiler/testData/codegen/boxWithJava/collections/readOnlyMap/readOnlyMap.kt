open class KMap<K, V> : Map<K, V> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsKey(key: K) = true
    override fun containsValue(value: V) = true

    override fun get(key: K): V? {
        throw UnsupportedOperationException()
    }

    override val keys: Set<K>
        get() = throw UnsupportedOperationException()
    override val values: Collection<V>
        get() = throw UnsupportedOperationException()
    override val entries: Set<Map.Entry<K, V>>
        get() = throw UnsupportedOperationException()
}

fun box() = J.foo()
