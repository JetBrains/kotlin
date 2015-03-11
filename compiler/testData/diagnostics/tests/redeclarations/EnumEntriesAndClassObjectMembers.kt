enum class E {
    <!REDECLARATION!>FIRST<!>

    <!REDECLARATION!>SECOND<!>

    default object {
        class <!REDECLARATION!>FIRST<!>

        val <!REDECLARATION!>SECOND<!> = this
    }
}
