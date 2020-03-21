class A {
    private inner class Inner
}
class B {
    <!FIR_EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias AInner = A.Inner<!>
    inner class Inner
}

class C {
    typealias BInner = B.Inner
}

<!FIR_EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias AInner0 = A.Inner<!>
typealias BInner0 = B.Inner