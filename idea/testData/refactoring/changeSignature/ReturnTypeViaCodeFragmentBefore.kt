class A<T, U>

interface I {
    fun <T, U> <caret>foo(): A<T, U>
}

class C : I {
    override fun <V, W> foo() = A<V, W>()
}