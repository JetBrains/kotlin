interface T

// SIBLING:
fun foo(): T {
    class A: T

    fun bar(): A {
        val a = A()
        return <selection>a</selection>
    }
}