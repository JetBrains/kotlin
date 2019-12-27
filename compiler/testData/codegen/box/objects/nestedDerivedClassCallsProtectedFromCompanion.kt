open class A {
    companion object {
        protected fun foo() = "OK"
    }
    class B : A() {
        fun bar() = foo()
    }
}

fun box() = A.B().bar()
