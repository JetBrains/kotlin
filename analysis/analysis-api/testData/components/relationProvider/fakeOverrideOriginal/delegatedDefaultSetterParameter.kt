package test

interface I {
    var foo: Int
}

class Delegating(impl: I) : I by impl

// value_parameter: value: setter: callable: test/Delegating.foo
