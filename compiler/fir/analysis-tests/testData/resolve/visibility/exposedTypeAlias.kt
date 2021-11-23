class A {
    private inner class Inner
}
class B {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>AInner<!> = <!INVISIBLE_REFERENCE!>A.Inner<!><!>
    inner class Inner
}

class C {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias BInner = B.Inner<!>
}

typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>AInner0<!> = <!INVISIBLE_REFERENCE!>A.Inner<!>
typealias BInner0 = B.Inner

private typealias MyString = String

fun foo(): MyString = ""
