interface P<U, Y>

class A<T> {
    class B {
        fun test() {
            class C<W>() : P<W, <!UNRESOLVED_REFERENCE!>T<!>> {
                companion object : P<W, <!UNRESOLVED_REFERENCE!>T<!>> {
                }

                inner class D : P<W, <!UNRESOLVED_REFERENCE!>T<!>>
            }
        }
    }
}