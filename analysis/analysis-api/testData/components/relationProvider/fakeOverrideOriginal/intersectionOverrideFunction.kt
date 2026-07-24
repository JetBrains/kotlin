package test

interface A {
    fun foo()
}

interface B {
    fun foo()
}

interface C : A, B

// function: test/C.foo
