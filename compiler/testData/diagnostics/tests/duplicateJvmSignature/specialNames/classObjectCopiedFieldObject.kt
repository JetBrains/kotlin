class B {
    default object <!REDECLARATION!>A<!> {
    }

    val <!REDECLARATION!>A<!> = this
}

class C {
    default <!CONFLICTING_JVM_DECLARATIONS!>object A<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val A<!> = this
    }

}

class D {
    default <!CONFLICTING_JVM_DECLARATIONS!>object<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val `OBJECT$`<!> = this
    }

    val `OBJECT$` = D
}