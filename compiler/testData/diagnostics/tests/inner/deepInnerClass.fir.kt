interface P<U, Y>

class A<T> {
    class B {
        fun test() {
            class C<W>() : <!UNRESOLVED_REFERENCE!>P<W, T><!> {
                companion object : <!UNRESOLVED_REFERENCE!>P<W, T><!> {
                }

                inner class D : <!UNRESOLVED_REFERENCE!>P<W, T><!>
            }
        }
    }
}