// FIR_IDENTICAL
// !MARK_DYNAMIC_CALLS

fun foo(d: dynamic) {
    if (d is Foo) {
        d.bar() // resolved statically
        d.<!DEBUG_INFO_DYNAMIC!>baz<!>()
    }
}

class Foo {
    fun bar() {}
}