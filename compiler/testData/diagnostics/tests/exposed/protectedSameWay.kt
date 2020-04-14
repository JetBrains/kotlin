// FIR_IDENTICAL
abstract class Outer {
    protected open class My
    // Both valid: same way protected
    protected class Your: My()
    abstract protected fun foo(my: My): Your
}

class OuterDerived: Outer() {
    // valid, My has better visibility
    protected class His: Outer.My()
    // valid, My and Your have better visibility
    <!EXPOSED_FUNCTION_RETURN_TYPE!>override fun foo(<!EXPOSED_PARAMETER_TYPE!>my: Outer.My<!>) = Outer.Your()<!>
}