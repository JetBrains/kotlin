// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

@Experimental
annotation class E

open class Base {
    @E
    open fun foo() {}
}

// FILE: usage.kt

package usage

import api.*

class Derived : Base() {
    override fun foo() {}
}

fun test(b: Base) {
    b.foo()
}
