class B {
    companion <!REDECLARATION!>object A<!> {
    }

    <!REDECLARATION!>val A = this<!>
}

class C {
    companion object A {
        val A = this
    }

}
