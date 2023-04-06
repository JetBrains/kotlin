// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57430

fun box(): String {
    return object {
        val a = A("OK")
        inner class A(val ok: String)
    }.a.ok
}
