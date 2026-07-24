package test

interface A {
    context(c: Int)
    fun foo()
}

interface B {
    context(c: Int)
    fun foo()
}

interface C : A, B

// context_parameter: c: function: test/C.foo
