package test

interface I<T> {
    var foo: T?
        get() = null
        set(value) {}
}

class Substituted: I<Int>

// value_parameter: value: setter: callable: test/Substituted.foo
