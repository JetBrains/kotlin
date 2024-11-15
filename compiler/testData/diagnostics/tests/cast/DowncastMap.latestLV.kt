// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
interface Map<K, out V>
interface MutableMap<K, V>: Map<K, V> {
  operator fun set(k: K, v: V)
}

fun p(p: Map<String, Int>) {
    if (p is <!CANNOT_CHECK_FOR_ERASED!>MutableMap<String, Int><!>) {
        p[""] = 1
    }
}