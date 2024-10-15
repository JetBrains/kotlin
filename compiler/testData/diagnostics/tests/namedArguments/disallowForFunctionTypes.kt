// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_TXT

fun test(f: (String) -> Unit) {
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")
}
