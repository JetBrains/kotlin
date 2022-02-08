// !LANGUAGE: +ContextReceivers
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Outer {
    val x: Int = 1
}

context(Outer)
class Inner(arg: Any) {
    fun bar() = <!UNRESOLVED_REFERENCE!>x<!>
}

fun f(outer: Outer) {
    Inner(1)
    with(outer) {
        Inner(3)
    }
}