// IGNORE_ANNOTATIONS

inline class InlineMapEntry<K, V>(private val e: Map.Entry<K, V>) : Map.Entry<K, V> {
    override val key: K get() = e.key
    override val value: V get() = e.value
}

