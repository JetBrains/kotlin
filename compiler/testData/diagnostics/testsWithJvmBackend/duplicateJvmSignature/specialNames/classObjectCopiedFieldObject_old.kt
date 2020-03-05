// TARGET_BACKEND: JVM_OLD

class B {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object <!REDECLARATION!>A<!><!> {
    }

    <!CONFLICTING_JVM_DECLARATIONS!>val <!REDECLARATION!>A<!>: A<!> = B.A
}

class C {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object A<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val A: A<!> = C.A
    }
}

class D {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object A<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>lateinit var A: A<!>
    }
}
