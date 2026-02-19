// IGNORE_FIR_DIAGNOSTICS
// IGNORE_ERRORS

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

<!CONFLICTING_JVM_DECLARATIONS!>class D {
    companion object A {
        lateinit <!CONFLICTING_JVM_DECLARATIONS!>var A: A<!>
    }
}<!>
