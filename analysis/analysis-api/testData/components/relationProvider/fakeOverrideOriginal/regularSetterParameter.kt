package test

interface I {
    var foo: Int
        get() = 5
        set(value) {}
}

// value_parameter: value: setter: callable: test/I.foo
