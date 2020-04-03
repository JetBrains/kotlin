package test

class EmptyMap<V> : Map<String, V> {
    override fun isEmpty() = true
    override val size: Int get() = 0
    override fun containsKey(key: String) = false
    override fun containsValue(value: V) = false
    override fun get(key: String): V? = null
    operator fun set(key: String, value: V): V? = null
    override val keys : MutableSet<String> = mutableSetOf()
    override val values: MutableCollection<V> = mutableSetOf()
    override val entries : MutableSet<MutableMap.MutableEntry<String, V>> = mutableSetOf()
}

