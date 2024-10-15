// RUN_PIPELINE_TILL: SOURCE
package a

fun foo(a: Any) = a <!DEBUG_INFO_MISSING_UNRESOLVED!>==<!><!SYNTAX!><!>
