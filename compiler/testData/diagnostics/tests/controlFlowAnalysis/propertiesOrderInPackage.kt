// RUN_PIPELINE_TILL: FRONTEND
package a

val a : Int = <!UNINITIALIZED_VARIABLE!>b<!>
val b : Int = a

/* GENERATED_FIR_TAGS: propertyDeclaration */
