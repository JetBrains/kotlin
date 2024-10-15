// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION
fun <T> T.mustBe(t : T) {
    "$this must be$<!SYNTAX!>as<!>$t"
}