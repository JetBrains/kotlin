// !DIAGNOSTICS: -UNUSED_EXPRESSION
fun <T> T.mustBe(t : T) {
    "$this must be$<!SYNTAX!>as<!>$t"
}