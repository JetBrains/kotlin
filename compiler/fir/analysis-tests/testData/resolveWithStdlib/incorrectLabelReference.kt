// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58439

fun myMap(x: Int = @<!UNRESOLVED_REFERENCE!>someLabel<!><!SYNTAX!><!>)

val y = (<!SYNTAX!><!SYNTAX!><!>:)<!>
