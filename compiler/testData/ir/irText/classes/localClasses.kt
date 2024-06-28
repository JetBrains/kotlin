// FIR_IDENTICAL

fun outer() {
    class LocalClass {
        fun foo() {}
    }
    LocalClass().foo()
}
