package p

import p.Foo.Nested

open class Foo {
    protected class Nested
}

class Bar: Foo() {
    <!EXPOSED_FUNCTION_RETURN_TYPE!>protected fun foo(): Nested? = null<!>
}

private fun foo(): Nested? = null
private fun bar(): p.Foo.Nested? = null