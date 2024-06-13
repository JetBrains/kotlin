// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class E

open class Base {
    @E
    open fun foo() {}
}

// FILE: usage.kt

package usage

import api.*

class Derived : Base() {
    override fun <!OPT_IN_OVERRIDE_ERROR!>foo<!>() {}
}

fun test(b: Base) {
    b.<!OPT_IN_USAGE_ERROR!>foo<!>()
}
