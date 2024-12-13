fun test(a: Any) {
    if (a !is Foo) {
        return
    }

    <expr>check</expr>(a.process())

    // Force partial body resolution
    check(true)
}

fun check(condition: Boolean) {}

interface Foo {
    fun process(): Boolean
}