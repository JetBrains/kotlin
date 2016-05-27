// FILE: Foo.kt

package test

class Foo {
    fun bar() {}
}

// FILE: test.kt

import test.Foo

fun Foo(): String = ""

val f = Foo::bar
val g = Foo::<!UNRESOLVED_REFERENCE!>length<!>
