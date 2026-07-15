package test

interface I {
    var foo: Int
        get() = 5
        set(value) {}
}

class Delegating(impl: I) : I by impl

// value_parameter: value: setter: callable: test/Delegating.foo
