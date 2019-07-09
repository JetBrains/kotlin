// WITH_RUNTIME
class A {
    fun foo() {}
}
fun baz(a: A): Int = 1

fun test() {
    val a = A()
    a.foo()
    1 + <caret>baz(a)
    a.foo()
}