// !API_VERSION: 1.3
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.ERROR, [Experimental.Impact.COMPILATION])
annotation class E

open class Base {
    @E
    open fun foo() {}
}

// MODULE: usage(api)
// FILE: usage.kt

package usage

import api.*

class Derived : Base() {
    override fun <!EXPERIMENTAL_OVERRIDE_ERROR!>foo<!>() {}
}

fun test(b: Base) {
    b.<!EXPERIMENTAL_API_USAGE_ERROR!>foo<!>()
}
