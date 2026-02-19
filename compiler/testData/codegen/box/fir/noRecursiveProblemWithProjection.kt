// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-6413

class A<in T> {
    private fun foo() = foo(1)
    private fun foo(i: Int) = "O"

    fun bar() = foo()
}

class B<out T> {
    private fun foo() = foo(2)
    private fun foo(i: Int) = "K"

    fun bar() = foo()
}

fun box(): String {
    return A<Any>().bar() + B<Any>().bar()
}
