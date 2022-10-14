interface A {
    fun <T>foo()
}

interface B {
    fun <T>foo() {}
}

<expr>class Y : A, B</expr>