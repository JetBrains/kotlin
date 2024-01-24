// IGNORE_ANNOTATIONS

inline class InlineMutableMap<K, V>(private val mmap: MutableMap<K, V>) : MutableMap<K, V> {
    override val size: Int get() = mmap.size
    override fun containsKey(key: K): Boolean = mmap.containsKey(key)
    override fun containsValue(value: V): Boolean = mmap.containsValue(value)
    override fun get(key: K): V? = mmap[key]
    override fun isEmpty(): Boolean = mmap.isEmpty()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = mmap.entries
    override val keys: MutableSet<K> get() = mmap.keys
    override val values: MutableCollection<V> get() = mmap.values
    override fun clear() { mmap.clear() }
    override fun put(key: K, value: V): V? = mmap.put(key, value)
    override fun putAll(from: Map<out K, V>) { mmap.putAll(from) }
    override fun remove(key: K): V? = mmap.remove(key)
}
