// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
class Aaa() {
    val <!REDECLARATION!>a<!> = 1
    val <!REDECLARATION!>a<!> = 1
}