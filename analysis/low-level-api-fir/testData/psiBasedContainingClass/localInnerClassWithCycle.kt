fun foo() {
    open class Local {
        open inner class A : C() {
            abstract inner class Inner
        }

        abstract inner class C : A.Inner()
    }
}
