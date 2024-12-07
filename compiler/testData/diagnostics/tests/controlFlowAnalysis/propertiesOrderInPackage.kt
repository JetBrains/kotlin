// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package a

val a : Int = <!UNINITIALIZED_VARIABLE!>b<!>
val b : Int = a
