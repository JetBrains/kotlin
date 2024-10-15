// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// SKIP_TXT

fun test(f: (String) -> Unit) {
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")
}
