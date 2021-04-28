interface A {
    sealed fun foo()
    sealed var bar: Unit
}

interface B {
    abstract fun foo()
    abstract var bar: Unit
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface C<!> : A, B

abstract class D(sealed var x: Int) {
    abstract var y: Unit
        sealed set
}

abstract class E : D(42)
