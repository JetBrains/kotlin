package test

interface I<T> {
    var foo: T?
}

interface Substituted: I<Int>

// value_parameter: value: setter: callable: test/Substituted.foo
