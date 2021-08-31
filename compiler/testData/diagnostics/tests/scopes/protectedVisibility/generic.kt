// FIR_IDENTICAL
abstract class Parent<F> {
    protected fun foo() {}
}

class Derived<E> : Parent<E>() {
    fun bar(x: Derived<String>) {
        x.foo()
    }
}
