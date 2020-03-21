class A {
    private inner class Inner
}
class B {
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias AInner = A.Inner<!>
    inner class Inner
}

class C {
    typealias BInner = B.Inner
}

<!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias AInner0 = A.Inner<!>
typealias BInner0 = B.Inner