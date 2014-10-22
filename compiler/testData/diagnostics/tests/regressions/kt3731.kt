// KT-3731 Resolve & inner class

class A {
    fun foo() {}
    fun bar(f: A.() -> Unit = {}) = f()
}

class B {
    class D {
        {
            A().bar {
                this.foo()
                foo()
            }
        }
    }
}
