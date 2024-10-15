// RUN_PIPELINE_TILL: SOURCE
package l

fun test(a: Int) {
    if (a <!UNRESOLVED_REFERENCE!>in<!><!SYNTAX!><!> ) {} //a is not unresolved
}
