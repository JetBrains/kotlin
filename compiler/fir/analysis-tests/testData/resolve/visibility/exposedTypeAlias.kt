class A {
    private inner class Inner
}
class B {
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>AInner<!> = A.Inner
    inner class Inner
}

class C {
    typealias BInner = B.Inner
}

typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>AInner0<!> = A.Inner
typealias BInner0 = B.Inner

private typealias MyString = String

fun foo(): MyString = ""