package test

open class A {
    var foo: Int = 5
}

interface B {
    var foo: Int
}

class C: A(), B

// value_parameter: value: setter: callable: test/C.foo
