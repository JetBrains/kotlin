interface A {
    operator fun plusAssign(i: Int)
}

interface MyMap<K, V> {
    operator fun get(k: K): V
}

fun test(m: MyMap<String, A>) {
    m["a"] <expr>+=</expr> 1
}