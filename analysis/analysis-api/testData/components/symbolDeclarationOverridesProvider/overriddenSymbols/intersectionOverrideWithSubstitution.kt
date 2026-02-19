// callable: /A.foo
interface A : B, C

interface B {
    fun foo(x: Int)
}

interface C : D<Int>

interface D<T> {
    fun foo(x: T)
}
