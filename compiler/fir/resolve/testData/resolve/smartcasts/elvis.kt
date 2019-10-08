interface A {
    fun foo()
    val b: Boolean
}

fun test_1(x: A?) {
    if (x?.b ?: return) {
        x.foo()
    }
}