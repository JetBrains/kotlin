// RUN_PIPELINE_TILL: FRONTEND
package l

fun test(a: Int) {
    if (a in<!SYNTAX!><!> ) {} //a is not unresolved
}
