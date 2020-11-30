// WITH_RUNTIME
open class ControlFlowInfo<K, V>(val map: Map<K, V>): Map<K, V> by map


class StringFlowInfo(map: Map<String, String>): ControlFlowInfo<String, String>(map) {
    fun foo(info: StringFlowInfo): String {
        if (keys.size == info.keys.size) return "OK"
        return "FAIL"
    }
}

fun box(): String {
    val s = StringFlowInfo(mapOf("a" to "b"))
    return s.foo(s)
}