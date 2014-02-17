class A {
    fun foo() {}
    fun bar(f: A.() -> Unit = {}) {}
}

class B {
    class D {
        {
            A().bar {
                this.foo()
            }
        }
    }
}

fun box(): String {
    B.D()
    return "OK"
}
