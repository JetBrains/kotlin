// FILE: a.kt

package foo

object Bar {
    fun bar() {}
}

// FILE: b.kt

package baz

import foo.Bar

class C: <!UNRESOLVED_REFERENCE!>Bar<!>

fun test() {
    Bar.bar()
}