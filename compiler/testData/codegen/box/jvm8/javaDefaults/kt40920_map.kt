// !JVM_DEFAULT_MODE: disable
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

// FILE: main.kt
var result = ""

interface A<K, V> : MutableMap<K, V>

interface Left<K, V> : A<K, V>

interface Right<K, V> : A<K, V> {

    override fun remove(key: K): V? {
        result += key.toString() + ";"
        return null
    }

    override public fun getOrDefault(key: K, defaultValue: V): V {
        result += key.toString() + ";"
        return defaultValue
    }
}


internal class MyMap : Left<String, String>, Right<String, String> {
    override val size: Int
        get() = null!!

    override fun isEmpty(): Boolean {
        return true
    }

    override fun containsKey(key: String): Boolean {
        return false
    }

    override fun containsValue(value: String): Boolean {
        return false
    }

    override fun get(key: String): String? {
        TODO("Not yet implemented")
    }

    override fun put(key: String, value: String): String? {
        result += "$key=$value;"
        return null
    }

    override fun putAll(from: Map<out String, String>) {
    }

    override fun clear() {}

    override val keys: MutableSet<String>
        get() = null!!

    override val values: MutableCollection<String>
        get() = null!!

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = null!!
}

fun box(): String {
    val map = MyMap()
    map["O"] = "fail"
    map.remove("O")

    val value = map.getOrDefault("O", "OK")
    if (result != "O=fail;O;O;") return "fail 3: $result"
    return value
}
