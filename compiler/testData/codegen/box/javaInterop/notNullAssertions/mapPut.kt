// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME

fun <K: Any, V: Any> foo(k: K, v: V) {
    val map = HashMap<K, V>()
    val old = map.put(k, v)
}

fun box(): String {
    foo("", "")
    return "OK"
}