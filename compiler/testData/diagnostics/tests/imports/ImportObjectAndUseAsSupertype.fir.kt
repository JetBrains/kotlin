// FILE: a.kt

package foo

object Bar {
    fun bar() {}
}

// FILE: b.kt

package baz

import foo.Bar

class C: Bar

fun test() {
    Bar.bar()
}