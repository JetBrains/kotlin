interface Map<K, out V>
interface MutableMap<K, V>: Map<K, V> {
  operator fun set(k: K, v: V)
}

fun p(p: Map<String, Int>) {
    if (p is MutableMap<String, Int>) {
        <!DEBUG_INFO_SMARTCAST!>p<!>[""] = 1
    }
}