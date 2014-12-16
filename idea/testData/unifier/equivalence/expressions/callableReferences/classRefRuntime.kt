class A {
    inner class B

    inner class C

    fun foo() {
        <selection>::B</selection>
        ::C
        A::B
        A::C
    }
}