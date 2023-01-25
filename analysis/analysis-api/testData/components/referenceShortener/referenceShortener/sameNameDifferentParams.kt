// FILE: main.kt

fun foo() {}

fun test() {
    fun foo(a: Int) = a + 1
    <expr>foo()</expr>
}