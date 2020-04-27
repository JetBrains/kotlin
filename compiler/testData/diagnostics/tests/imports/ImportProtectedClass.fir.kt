package p

import p.Foo.Nested

open class Foo {
    protected class Nested
}

class Bar: Foo() {
    protected fun foo(): Nested? = null
}

private fun foo(): Nested? = null
private fun bar(): p.Foo.Nested? = null