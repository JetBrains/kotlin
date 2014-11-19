// !MARK_DYNAMIC_CALLS

// MODULE[js]: m1
// FILE: k.kt

fun foo(d: dynamic) {
    if (d is Foo) {
        d.bar() // resolved statically
        d.<!DEBUG_INFO_DYNAMIC!>baz<!>()
    }
}

class Foo {
    fun bar() {}
}