package test

interface A<T> {
    fun foo(xx: T)
}

interface B : A<Int>

// value_parameter: xx: function: test/B.foo
