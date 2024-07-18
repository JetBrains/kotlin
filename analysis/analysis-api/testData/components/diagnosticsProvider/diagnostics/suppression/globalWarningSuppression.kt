// SUPPRESS_WARNINGS: REDUNDANT_PROJECTION, UNCHECKED_CAST

class Out<out T> {
    fun foo() {}
}

fun test_1(x: Out<out Int>): Out<String> {
    return x as Out<String>
}

fun test_2(x: String) {
    x as String
}
