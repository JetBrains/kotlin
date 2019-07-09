// IS_APPLICABLE: false
class A {
    fun foo() {}
}

fun test() {
    val a = A()
    a.foo()
    <caret>1 + 1
    a.foo()
}