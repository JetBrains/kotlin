class A {
    private inner class Inner
}
class B {
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE{LT}!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE{PSI}!>AInner<!> = A.Inner<!>
    inner class Inner
}

class C {
    typealias BInner = B.Inner
}

<!EXPOSED_TYPEALIAS_EXPANDED_TYPE{LT}!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE{PSI}!>AInner0<!> = A.Inner<!>
typealias BInner0 = B.Inner

private typealias MyString = String

fun foo(): MyString = ""
