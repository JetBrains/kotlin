// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Deprecated("No")
val f: () -> Unit = {}

fun test() {
    <!DEPRECATION!>f<!>()
}
