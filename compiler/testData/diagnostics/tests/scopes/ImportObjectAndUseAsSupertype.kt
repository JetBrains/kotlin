// FILE: a.kt

package foo

object Bar {
    fun bar() {}
}

// FILE: b.kt

package baz

import foo.Bar

class C: <!SUPERTYPE_NOT_INITIALIZED, FINAL_SUPERTYPE!>Bar<!>

fun test() {
    Bar.bar()
}