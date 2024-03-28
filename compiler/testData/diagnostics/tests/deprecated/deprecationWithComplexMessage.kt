// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
const val MSG = "Hello" + " World"

@Deprecated(MSG)
fun deprecated() {}

fun test() {
    <!DEPRECATION!>deprecated<!>()
}