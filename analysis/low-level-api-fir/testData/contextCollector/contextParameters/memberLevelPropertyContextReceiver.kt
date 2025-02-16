package one

class A

class B {
    context(A, Int)
    val String.foo get() {
        <expr>val x = 1</expr>
    }
}

// LANGUAGE: +ContextReceivers
