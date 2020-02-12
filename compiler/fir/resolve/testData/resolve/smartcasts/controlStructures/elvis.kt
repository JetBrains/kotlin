interface A {
    fun foo()
    val b: Boolean
}

fun test_1(x: A?) {
    if (x?.b ?: return) {
        x.foo()
    }
}

fun test2(a: Any?, b: Any?): String {
    if (b !is String) return ""
    if (a !is String?) return ""
    return a ?: b
}