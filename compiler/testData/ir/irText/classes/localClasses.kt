// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57430

fun outer() {
    class LocalClass {
        fun foo() {}
    }
    LocalClass().foo()
}
