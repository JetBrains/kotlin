// FIR_IDENTICAL

interface IFoo {
    fun foo()
    fun bar() { foo() }
}