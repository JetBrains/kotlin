fun Any.test() {
    if (this is String) return
    <expr>foo</expr>()
}

fun Any.foo() {}
