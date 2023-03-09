// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57429

class Outer<T1> {
    inner class Inner<T2> {
        fun foo(x1: T1, x2: T2) {}
    }
}
