// LL_FIR_DIVERGENCE
// KT-62587
// LL_FIR_DIVERGENCE
// FIR_IDENTICAL
annotation class Anno(val number: Int)

class Outer {
    companion object {
        const val CONSTANT_FROM_COMPANION = 42

        @Anno(<!UNRESOLVED_REFERENCE!>CONSTANT_FROM_COMPANION<!>)
        class Nested
    }
}
