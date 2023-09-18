interface A {
    operator fun inc(): A
}

interface MyMap<K, V> {
    operator fun get(k: K): V
    operator fun set(k: K, v: V): Unit
}

fun test(m: MyMap<String, A>) {
    <expr>m["a"]</expr>++
}