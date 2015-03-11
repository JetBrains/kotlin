class C {
    default <!REDECLARATION!>object<!> {
        val <!REDECLARATION!>Default<!> = this
    }

    val <!REDECLARATION!>Default<!> = C
}

class D {
    default <!CONFLICTING_JVM_DECLARATIONS!>object<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val `OBJECT$`<!> = this
    }

    val `OBJECT$` = D
}