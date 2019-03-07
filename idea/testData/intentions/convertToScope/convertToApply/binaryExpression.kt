// WITH_RUNTIME
class A {
    fun foo() {}
    fun bar(): Int = 1
}

fun test() {
    val a = A()
    a.foo()
    <caret>a.bar() + 1
    a.foo()
}