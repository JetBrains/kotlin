// getter: callable: /A.foo
interface A : B, C

interface B {
    val foo: Int
}

interface C : D {
    override val foo: Int
}

interface D {
    val foo: Int
}
