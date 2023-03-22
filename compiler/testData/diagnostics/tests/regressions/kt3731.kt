// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// KT-3731 Resolve & inner class

class A {
    fun foo() {}
    fun bar(f: A.() -> Unit = {}) = f()
}

class B {
    class D {
        init {
            A().bar {
                this.foo()
                foo()
            }
        }
    }
}
