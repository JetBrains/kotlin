// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn
annotation class E

open class Base {
    @E
    open fun foo() {}
}

// FILE: usage.kt

package usage

import api.*

class Derived : Base() {
    override fun <!EXPERIMENTAL_OVERRIDE_ERROR!>foo<!>() {}
}

fun test(b: Base) {
    b.<!EXPERIMENTAL_API_USAGE_ERROR!>foo<!>()
}
