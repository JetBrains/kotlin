// FIR_IDENTICAL

class Outer<T1> {
    inner class Inner<T2> {
        fun foo(x1: T1, x2: T2) {}
    }
}
