interface MyMap<K, V> {
    operator fun get(k: K): V
    operator fun set(k: K, v: V)
}

fun test(m: MyMap<String, Int>) {
    <expr>m</expr>["a"] += 1
}