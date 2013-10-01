class MyMap<K, V>: Map<K, V> {
    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun containsKey(key: Any?): Boolean = false
    override fun containsValue(value: Any?): Boolean = false
    override fun get(key: Any?): V? = null
    override fun keySet(): Set<K> = throw UnsupportedOperationException()
    override fun values(): Collection<V> = throw UnsupportedOperationException()
    override fun entrySet(): Set<Map.Entry<K, V>> = throw UnsupportedOperationException()

    public fun put(key: K, value: V): V? = null
    public fun remove(key: Any?): V? = null
    public fun putAll(m: Map<out K, V>) {}
    public fun clear() {}
}

fun box(): String {
    val map = MyMap<String, Int>() as MutableMap<String, Int>

    map.put("", 1)
    map.remove("")
    map.putAll(map)
    map.clear()

    return "OK"
}

