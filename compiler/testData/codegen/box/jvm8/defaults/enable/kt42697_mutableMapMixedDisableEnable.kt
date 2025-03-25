// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// MODULE: lib
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt

abstract class Base<K, V> : MutableMap<K, V>

// MODULE: box(lib)
// JVM_DEFAULT_MODE: enable
// FILE: box.kt

var result = ""

interface A<K, V> : MutableMap<K, V> {
    override fun remove(key: K): V? {
        result += "remove;"
        return null
    }

    override public fun getOrDefault(key: K, defaultValue: V): V {
        result += "getOrDefault;"
        return defaultValue
    }
}

class MyMap : Base<String, String>(), A<String, String> {
    override val size: Int get() = null!!
    override fun isEmpty(): Boolean = true
    override fun containsKey(key: String): Boolean = false
    override fun containsValue(value: String): Boolean = false
    override fun get(key: String): String? = null
    override fun put(key: String, value: String): String? {
        result += "put;"
        return null
    }
    override fun putAll(from: Map<out String, String>) {}
    override fun clear() {}
    override val keys: MutableSet<String> get() = null!!
    override val values: MutableCollection<String> get() = null!!
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>> get() = null!!
}

fun box(): String {
    val map = MyMap()
    map["1"] = "2"
    map.remove("1")
    val value = map.getOrDefault("3", "OK")
    if (result != "put;remove;getOrDefault;") return "Fail: $result"
    return value
}
