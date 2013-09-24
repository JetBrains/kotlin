class MyMap<K, V>: Map<K, V> {
    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun containsKey(key: Any?): Boolean = false
    override fun containsValue(value: Any?): Boolean = false
    override fun get(key: Any?): V? = null
    override fun keySet(): Set<K> = throw UnsupportedOperationException()
    override fun values(): Collection<V> = throw UnsupportedOperationException()
    override fun entrySet(): Set<Map.Entry<K, V>> = throw UnsupportedOperationException()
}

fun expectUoe(block: () -> Unit) {
    try {
        block()
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
    }
}

fun box(): String {
    val map = MyMap<String, Int>() as MutableMap<String, Int>

    expectUoe { map.put("", 1) }
    expectUoe { map.remove("") }
    expectUoe { map.putAll(map) }
    expectUoe { map.clear() }

    return "OK"
}

