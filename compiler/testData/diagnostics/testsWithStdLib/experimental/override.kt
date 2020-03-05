// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class E

open class Base {
    @E
    open fun foo() {}
}

class DerivedInSameModule : Base() {
    override fun <!EXPERIMENTAL_OVERRIDE!>foo<!>() {}
}

// FILE: usage-propagate.kt

package usage1

import api.*

open class Derived : Base() {
    @E
    override fun foo() {}
}

class SubDerived : Derived()

@E
class Derived2 : Base() {
    override fun foo() {}
}

// FILE: usage-none.kt

package usage2

import api.*

class Derived : Base() {
    override fun <!EXPERIMENTAL_OVERRIDE!>foo<!>() {}
}
