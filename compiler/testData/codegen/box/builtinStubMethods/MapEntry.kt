class MyMapEntry<K, V>: Map.Entry<K, V> {
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = false
    override fun getKey(): K = throw UnsupportedOperationException()
    override fun getValue(): V = throw UnsupportedOperationException()
}

fun box(): String {
    try {
        (MyMapEntry<String, Int>() as MutableMap.MutableEntry<String, Int>).setValue(1)
        throw AssertionError()
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
