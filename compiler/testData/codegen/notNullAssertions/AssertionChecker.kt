class AssertionChecker(val illegalStateExpected: Boolean) {
    fun invoke(name: String, f: () -> Unit) {
        try {
            f()
        } catch (e: IllegalStateException) {
            if (!illegalStateExpected) throw AssertionError("Unexpected IllegalStateException on calling $name")
            return
        }
        if (illegalStateExpected) throw AssertionError("IllegalStateException expected on calling $name")
    }
}


trait Tr { 
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
    
    // postfix expression
    check("inc") { var a = A(); a++ }
    
    // prefix expression
    check("inc") { var a = A(); ++a }
    
    // field
    check("NULL") { val a = A().NULL }
    
    // static field
    check("STATIC_NULL") { val a = A.STATIC_NULL }
}
