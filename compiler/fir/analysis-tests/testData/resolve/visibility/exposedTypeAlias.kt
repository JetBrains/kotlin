// RUN_PIPELINE_TILL: FRONTEND
class A {
    private inner class Inner
}
class B {
    <!UNSUPPORTED_FEATURE!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>AInner<!> = A.<!INVISIBLE_REFERENCE!>Inner<!><!>
    inner class Inner
}

class C {
    <!UNSUPPORTED_FEATURE!>typealias BInner = B.Inner<!>
}

typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>AInner0<!> = A.<!INVISIBLE_REFERENCE!>Inner<!>
typealias BInner0 = B.Inner

private typealias MyString = String

fun foo(): MyString = ""
