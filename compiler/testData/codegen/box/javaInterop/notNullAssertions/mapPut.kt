// IGNORE_BACKEND: JS_IR

fun <K: Any, V: Any> foo(k: K, v: V) {
    val map = HashMap<K, V>()
    val old = map.put(k, v)
}

fun box(): String {
    foo("", "")
    return "OK"
}