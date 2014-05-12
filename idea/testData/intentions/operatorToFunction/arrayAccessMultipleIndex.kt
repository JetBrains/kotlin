class Bar {
    fun get(vararg args: Int) {}
}

fun foo(a: Bar, i: Int) {
    a<caret>[i, 1]
}
