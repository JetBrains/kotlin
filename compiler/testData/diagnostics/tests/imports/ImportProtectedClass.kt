package p

import p.Foo.Nested

open class Foo {
    protected class Nested
}

class Bar: Foo() {
    fun foo(): Nested? = null
}

fun foo(): <!INVISIBLE_REFERENCE!>Nested<!>? = null
fun bar(): p.Foo.<!INVISIBLE_REFERENCE!>Nested<!>? = null