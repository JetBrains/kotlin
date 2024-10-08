// RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_IDENTICAL

fun test(lambda: Int.(s: String) -> Unit) {
    lambda(<!NO_VALUE_FOR_PARAMETER!>1)<!>
}