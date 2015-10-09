class MyMap<K, V>: Map<K, V> {
    override val size: Int get() = 0
    override val isEmpty: Boolean get() = true
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
    val myMap = MyMap<String, Int>()
    val map = myMap as java.util.Map<String, Int>

    map.put("", 1)
    map.remove("")
    map.putAll(myMap)
    map.clear()

    return "OK"
}

