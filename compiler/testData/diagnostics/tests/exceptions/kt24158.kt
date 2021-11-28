// FIR_IDENTICAL
// SKIP_TXT

fun main() {
    null + <!SYNTAX!>$foo<!>.<!SYNTAX!>$bar<!>.<!SYNTAX!><!>
}

fun foo2() {
    null + <!SYNTAX!>$foo<!>. <!SYNTAX!>$bar<!> . <!SYNTAX!>$baz<!> .<!SYNTAX!><!>
}
