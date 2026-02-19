// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtThisExpression
fun foo(action: MyMap<String, Int>.() -> Unit) {
    foo {
        <expr>this</expr>["a"] += 1
    }
}

interface MyMap<K, V> {
    operator fun get(k: K): V
    operator fun set(k: K, v: V)
}
