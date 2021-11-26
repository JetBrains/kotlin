package p

import p.Foo.Nested

open class Foo {
    protected class Nested
}

class Bar: Foo() {
    protected fun foo(): Nested? = null
}

private fun foo(): <!INVISIBLE_REFERENCE!>Nested<!>? = null
private fun bar(): <!INVISIBLE_REFERENCE!>p.Foo.Nested<!>? = null
