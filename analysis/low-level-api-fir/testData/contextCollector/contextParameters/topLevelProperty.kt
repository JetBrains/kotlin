package one

class A

context(a: A, _: Int)
val foo: Unit
    get() {
        <expr>val x = 1</expr>
}
// LANGUAGE: +ContextParameters
