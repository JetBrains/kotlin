// RUN_PIPELINE_TILL: SOURCE
package a

fun foo() {
    val a = <!UNRESOLVED_REFERENCE!>getErrorType<!>()
    if (a == null) { //no senseless comparison

    }
}
