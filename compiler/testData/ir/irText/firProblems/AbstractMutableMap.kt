// WITH_RUNTIME
// FULL_JDK
// JVM_TARGET: 1.8

class MyMap<K : Any, V : Any> : AbstractMutableMap<K, V>() {
    override fun put(key: K, value: V): V? = null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = mutableSetOf()
}
