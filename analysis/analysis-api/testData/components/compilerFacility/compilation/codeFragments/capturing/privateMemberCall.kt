class A {
    private fun foo() = 2
}

fun test(a: A) {
    <caret>val x = 0
}
