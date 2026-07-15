package test

interface A {
    fun foo(xx: Int)
}

interface B {
    fun foo(xx: Int)
}

interface C : A, B

// value_parameter: xx: function: test/C.foo
