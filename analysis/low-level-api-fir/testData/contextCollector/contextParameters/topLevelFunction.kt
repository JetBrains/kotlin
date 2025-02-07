package one

class A

context(a: A, _: Int)
fun foo(p: Boolean) {
    <expr>val x = 1</expr>
}
// LANGUAGE: +ContextParameters
