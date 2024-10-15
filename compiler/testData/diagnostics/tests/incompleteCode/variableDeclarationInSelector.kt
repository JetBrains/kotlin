// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun foo(s: String) {
    s.<!SYNTAX!><!>
    val b = 42
}
