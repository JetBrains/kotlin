// callable: /A.foo
interface A : B, C

interface B {
    fun foo(x: Int)
}

interface C : D {
    override fun foo(x: Int)
}

interface D {
    fun foo(x: Int)
}
