class B {
    companion object <!REDECLARATION!>A<!> {
    }

    val <!REDECLARATION!>A<!> = this
}

class C {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object A<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val A<!> = this
    }

}

class D {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val `OBJECT$`<!> = this
    }

    val `OBJECT$` = D
}