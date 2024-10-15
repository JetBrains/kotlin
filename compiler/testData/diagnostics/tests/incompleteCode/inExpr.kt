// RUN_PIPELINE_TILL: SOURCE
package l

fun test(a: Int) {
    if (a in<!SYNTAX!><!> ) {} //a is not unresolved
}
