class C {
    <!REDECLARATION!>class object<!> {}

    val <!REDECLARATION!>Default<!> = C
}

class D {
    <!CONFLICTING_JVM_DECLARATIONS!>class object<!> {}

    <!CONFLICTING_JVM_DECLARATIONS!>val `OBJECT$`<!> = D
}