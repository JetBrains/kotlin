// KT-15642
open class Base {
    init { f() }
    open fun f() { }
}

class Derived : Base() {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val s = "Hello"<!>
    override fun f() { s.hashCode() }
}


