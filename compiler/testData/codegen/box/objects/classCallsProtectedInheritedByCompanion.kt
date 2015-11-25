open class A {
    protected fun foo() = "OK"
}

class B {
    companion object : A()

    fun bar() = foo()
}

fun box() = B().bar()
