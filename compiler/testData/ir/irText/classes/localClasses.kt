// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57430

fun outer() {
    class LocalClass {
        fun foo() {}
    }
    LocalClass().foo()
}
