// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// KT-9601 Chose maximally specific function in callable reference

open class A {
    fun foo(a: Any) {}
    fun fas(a: Int) {}
}
class B: A() {
    fun foo(a: Int) {}
    fun fas(a: Any) {}
}

fun test() {
    B::foo
    B::fas
}