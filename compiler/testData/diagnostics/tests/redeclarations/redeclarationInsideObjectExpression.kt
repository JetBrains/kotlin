// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
val x = object {
    val <!REDECLARATION!>y<!> = 1
    val <!REDECLARATION!>y<!> = 2
}