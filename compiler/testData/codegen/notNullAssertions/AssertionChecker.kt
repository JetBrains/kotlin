class AssertionChecker(val illegalStateExpected: Boolean) {
    operator fun invoke(name: String, f: () -> Any) {
        try {
            f()
        } catch (e: IllegalStateException) {
            if (!illegalStateExpected) throw AssertionError("Unexpected IllegalStateException on calling $name")
            return
        }
        if (illegalStateExpected) throw AssertionError("IllegalStateException expected on calling $name")
    }
}


interface Tr {
    fun foo(): String
}

class Derived : A(), Tr {
    override fun foo() = super<A>.foo()
}

class Delegated : Tr by Derived() {
}


fun checkAssertions(illegalStateExpected: Boolean) {
    val check = AssertionChecker(illegalStateExpected)
    
    // simple call
    check("foo") { A().foo() }
    
    // simple static call
    check("staticFoo") { A.staticFoo() }
    
    // supercall
    check("foo") { Derived().foo() }
    
    // delegated call
    check("foo") { Delegated().foo() }
    
    // collection element
    check("get") { A()[""] }
    
    // binary expression
    check("plus") { A() + A() }
    
    // field
    check("NULL") { A().NULL }
    
    // static field
    check("STATIC_NULL") { A.STATIC_NULL }

    // postfix expression
    // TODO:
//    check("inc") { var a = A().a(); a++ }

    // prefix expression
    check("inc-b") { var a = A.B.b(); a++ }

    // prefix expression
    check("inc-c") { var a = A.C.c(); a++ }

    // prefix expression
    check("inc") { var a = A().a(); ++a }

    // prefix expression
    check("inc-b") { var a = A.B.b(); ++a }

    // prefix expression
    // TODO:
//    check("inc-c") { var a = A.C.c(); ++a }
}

operator fun A.C.inc(): A.C = A.C()
operator fun <T> T.inc(): T = null as T
