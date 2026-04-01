// setter: callable: /A.foo
interface A : B, C

interface B {
    var foo: Int
}

interface C : D {
    override var foo: Int
}

interface D {
    var foo: Int
}
