interface A {
    fun foo()
}

open class B(private val a: A) : A by a

class C(a: A) : B(a) {
    override fun foo() {
        // Should be resolved to delegated B.foo (no error)
        super.foo()
    }
}
