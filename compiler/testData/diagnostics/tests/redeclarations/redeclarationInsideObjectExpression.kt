// FIR_IDENTICAL
val x = object {
    val <!REDECLARATION!>y<!> = 1
    val <!REDECLARATION!>y<!> = 2
}