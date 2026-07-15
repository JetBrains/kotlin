package test

interface A {
    fun Int.foo()
}

interface B {
    fun Int.foo()
}

interface C : A, B

// receiver_parameter: function: test/C.foo
