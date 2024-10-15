// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

fun main() {
     val list = <!UNRESOLVED_REFERENCE!>mutable<!> ListOf<!SYNTAX!><<!>Int<!SYNTAX!><!SYNTAX!>><!>(1) {}<!>
}
