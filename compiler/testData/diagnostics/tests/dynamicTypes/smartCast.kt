// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

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