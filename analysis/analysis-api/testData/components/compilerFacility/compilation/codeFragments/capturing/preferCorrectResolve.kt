class A {
    private fun foo(x: Int) = 1 + x
    fun foo(x: Any) = 2
}

fun test(a: A) {
    <caret>val x = 0
}
