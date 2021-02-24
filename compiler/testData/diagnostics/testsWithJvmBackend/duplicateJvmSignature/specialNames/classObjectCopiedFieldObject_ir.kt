// TARGET_BACKEND: JVM_IR

class B {
    companion object <!REDECLARATION!>A<!> {
    }

    val <!REDECLARATION!>A<!>: A = B.A
}

class C {
    companion object A {
        val A: A = C.A
    }
}

class <!CONFLICTING_JVM_DECLARATIONS!>D<!> {
    companion object A {
        <!CONFLICTING_JVM_DECLARATIONS!>lateinit var A: A<!>
    }
}
