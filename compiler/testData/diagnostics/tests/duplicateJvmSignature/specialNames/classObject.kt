class C {
    companion <!REDECLARATION!>object<!> {}

    val <!REDECLARATION!>Companion<!> = C
}

class D {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object<!> {}

    <!CONFLICTING_JVM_DECLARATIONS!>val `OBJECT$`<!> = D
}