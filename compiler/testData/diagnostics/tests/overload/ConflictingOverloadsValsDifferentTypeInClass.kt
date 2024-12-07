// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class Aaa() {
    val <!REDECLARATION!>a<!> = 1
    val <!REDECLARATION!>a<!> = ""
}