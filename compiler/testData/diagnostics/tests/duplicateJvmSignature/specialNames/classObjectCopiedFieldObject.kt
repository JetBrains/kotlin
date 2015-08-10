class B {
    companion object <!REDECLARATION!>A<!> {
    }

    val <!REDECLARATION!>A<!> = <!DEBUG_INFO_LEAKING_THIS!>this<!>
}

class C {
    companion <!CONFLICTING_JVM_DECLARATIONS!>object A<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val A<!> = <!DEBUG_INFO_LEAKING_THIS!>this<!>
    }

}
