package one

class A

class B {
    context(b: B, _: Int)
    val foo: Unit
        get() {
            <expr>val x = 1</expr>
    }
}
// LANGUAGE: +ContextParameters
