import Outer.Inner

open class Outer {
    open inner class Inner
}

<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>class Test: <!SUPERTYPE_NOT_INITIALIZED!>Inner<!> {
    fun foo() {}
}<!>
