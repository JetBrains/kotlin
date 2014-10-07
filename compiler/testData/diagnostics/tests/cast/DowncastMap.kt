trait Map<K, out V>
trait MutableMap<K, V>: Map<K, V> {
  fun set(k: K, v: V)
}

fun p(p: Map<String, Int>) {
    if (p is MutableMap<String, Int>) {
        <!DEBUG_INFO_SMARTCAST!>p<!>[""] = 1
    }
}