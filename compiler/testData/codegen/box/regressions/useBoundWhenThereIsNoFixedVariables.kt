fun <X : Y, Y> X.foo(y: Y) {}

fun test(x: (Int) -> Unit) {
    x.foo { it.toByte() }
}

fun box(): String {
    test { it -> }
    return "OK"
}