// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo { // also, it's important that Foo doesn't override equals
    fun foo()
}

fun check(x1: Foo, x: Any) {
    if (x1 == x) {
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
