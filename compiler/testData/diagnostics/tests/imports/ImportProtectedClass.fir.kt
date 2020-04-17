package p

import p.Foo.Nested

open class Foo {
    protected class Nested
}

class Bar: Foo() {
    protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(): Nested? = null
}

private fun foo(): Nested? = null
private fun bar(): p.Foo.Nested? = null