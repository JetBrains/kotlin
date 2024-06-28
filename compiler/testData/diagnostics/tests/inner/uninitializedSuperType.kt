import Outer.Inner

open class Outer {
    open inner class Inner
}

class Test : <!SUPERTYPE_NOT_INITIALIZED!>Inner<!> {
    fun foo() {}
}
