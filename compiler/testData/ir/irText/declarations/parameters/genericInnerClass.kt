// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

class Outer<T1> {
    inner class Inner<T2> {
        fun foo(x1: T1, x2: T2) {}
    }
}
