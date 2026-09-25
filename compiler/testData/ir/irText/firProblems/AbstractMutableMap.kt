// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM

// K1 kotlin-reflect drops the definitely non-nullable marker (`V & Any`) from the predefined enhancement of `java.util.Map.merge`
// and `computeIfPresent`, while the new implementation matches the compiler
// KOTLIN_REFLECT_DUMP_MISMATCH

class MyMap<K : Any, V : Any> : AbstractMutableMap<K, V>() {
    override fun put(key: K, value: V): V? = null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = mutableSetOf()
}
