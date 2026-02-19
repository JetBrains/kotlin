// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtReferenceExpression
fun foo(action: MyMap<String, A>.() -> Unit) {
    foo {
        <expr>this</expr>["a"] += 1
    }
}

interface A {
    operator fun plusAssign(i: Int)
}

interface MyMap<K, V> {
    operator fun get(k: K): V
}
