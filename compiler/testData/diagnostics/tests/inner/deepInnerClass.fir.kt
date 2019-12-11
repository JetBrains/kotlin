interface P<U, Y>

class A<T> {
    class B {
        fun test() {
            class C<W>() : P<W, T> {
                companion object : P<W, T> {
                }

                inner class D : P<W, T>
            }
        }
    }
}