// IGNORE_ANNOTATIONS

inline class InlineMap<K, V>(private val map: Map<K, V>) : Map<K, V> {
    override val entries: Set<Map.Entry<K, V>> get() = map.entries
    override val keys: Set<K> get() = map.keys
    override val size: Int get() = map.size
    override val values: Collection<V> get() = map.values
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map[key]
    override fun isEmpty(): Boolean = map.isEmpty()
}
