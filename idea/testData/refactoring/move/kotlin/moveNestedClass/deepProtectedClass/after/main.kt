class A {
    open class B {
        protected class D

        protected class C {
            private val d = D()
        }
    }
}

class X : A.B() {
    private val c = A.B.C()
}