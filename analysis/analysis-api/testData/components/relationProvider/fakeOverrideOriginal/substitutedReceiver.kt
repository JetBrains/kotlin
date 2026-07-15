package test

interface A<T> {
    fun T.foo()
}

interface B : A<Int>

// receiver_parameter: function: test/B.foo
