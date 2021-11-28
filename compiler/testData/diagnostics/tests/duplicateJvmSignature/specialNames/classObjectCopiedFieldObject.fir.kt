class B {
    companion object <!REDECLARATION!>A<!> {
    }

    val <!REDECLARATION!>A<!> = this
}

class C {
    companion object A {
        val A = this
    }

}
