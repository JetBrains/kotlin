// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
package a

val a : Int = <!UNINITIALIZED_VARIABLE!>b<!>
val b : Int = a
