// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
fun foo(s: String) {
    s.<!SYNTAX!><!>
    val b = 42
}
