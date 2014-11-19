// !MARK_DYNAMIC_CALLS

// MODULE[js]: m1
// FILE: k.kt

fun foo(d: dynamic) {
    Foo(d).p.<!DEBUG_INFO_DYNAMIC!>bar<!>()

}

class Foo<T>(val p: T)