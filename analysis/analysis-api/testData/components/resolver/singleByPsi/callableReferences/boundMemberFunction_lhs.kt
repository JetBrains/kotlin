interface Foo {
    fun foo(a: Int)
}

fun test(obj: Foo) {
    consume(<expr>obj</expr>::foo)
}

fun consume(f: (Int) -> Unit) {}