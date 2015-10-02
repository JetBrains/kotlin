class MyMapEntry<K, V>: Map.Entry<K, V> {
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = false
    override fun getKey(): K = throw UnsupportedOperationException()
    override fun getValue(): V = throw UnsupportedOperationException()

    public fun setValue(value: V): V = value
}

fun box(): String {
    (MyMapEntry<String, Int>() as java.util.Map.Entry<String, Int>).setValue(1)

    return "OK"
}
