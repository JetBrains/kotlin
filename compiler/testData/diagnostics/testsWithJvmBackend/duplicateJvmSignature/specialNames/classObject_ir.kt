// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR

class C {
    companion <!REDECLARATION!>object<!> {}

    val <!REDECLARATION!>Companion<!> = C
}
