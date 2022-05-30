// FIR_IDENTICAL
// SKIP_TXT

sealed class Outer {
    class NestedSubClass : Outer() {
        fun foo() {
            Inner()
        }
    }

    private inner class Inner
}
