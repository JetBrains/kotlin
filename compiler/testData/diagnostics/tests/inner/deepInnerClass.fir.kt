interface P<U, Y>

class A<T> {
    class B {
        fun test() {
            class C<W>() : <!OTHER_ERROR!>P<W, T><!> {
                companion object : <!OTHER_ERROR!>P<W, T><!> {
                }

                inner class D : <!OTHER_ERROR!>P<W, T><!>
            }
        }
    }
}