interface MyMap<K1, V1> {
    operator fun get(k: K1): V1
}

interface Foo {
    operator fun <K2, V2> MyMap<K2, V2>.set(k: K2, v: V2)

    fun test(m: MyMap<String, Int>) {
        <expr>++m["a"]</expr>
    }
}