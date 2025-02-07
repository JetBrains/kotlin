package one

class A

class B {
    context(a: A, _: Int)
    val foo: Unit
        get() {
            <expr>val x = 1</expr>
    }
}
// LANGUAGE: +ContextParameters
