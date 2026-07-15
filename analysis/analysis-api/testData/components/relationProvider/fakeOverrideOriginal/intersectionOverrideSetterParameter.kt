package test

open class A {
    var foo: Int
        get() = 5
        set(value) {}
}

interface B {
    var foo: Int
}

class C: A(), B

// value_parameter: value: setter: callable: test/C.foo
