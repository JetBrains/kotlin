// IGNORE_ANNOTATIONS

inline class InlineMutableMapEntry<K, V>(private val e: MutableMap.MutableEntry<K, V>) : MutableMap.MutableEntry<K, V> {
    override val key: K get() = e.key
    override val value: V get() = e.value
    override fun setValue(newValue: V): V = e.setValue(newValue)
}