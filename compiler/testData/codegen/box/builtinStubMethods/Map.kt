class MyMap<K, V>: Map<K, V> {
    override val size: Int get() = 0
    override val isEmpty: Boolean get() = true
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
    val myMap = MyMap<String, Int>()
    val map = myMap as java.util.Map<String, Int>

    expectUoe { map.put("", 1) }
    expectUoe { map.remove("") }
    expectUoe { map.putAll(myMap) }
    expectUoe { map.clear() }

    return "OK"
}

