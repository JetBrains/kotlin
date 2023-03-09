// IGNORE_ANNOTATIONS

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies
// (in this case, it's `remove-impl`/`getOrDefault-impl` because of inline class mangling)

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
