// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun foo(s: String) {
    s.<!SYNTAX!><!>
    val b = 42
}
