package one

class A

class B {
    context(a: A, _: Int)
    fun String.foo(p: Boolean) {
        <expr>val x = 1</expr>
    }
}

// LANGUAGE: +ContextParameters
