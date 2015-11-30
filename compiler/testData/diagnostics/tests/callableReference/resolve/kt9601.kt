// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

open class A
class B: A()

fun A.foo() {}
fun B.foo() {} // more specific

fun bar(a: Any) {}
fun bar(a: Int) {}  // more specific

fun test() {
    B::foo
    ::bar
}