// TARGET_BACKEND: JVM
open class C {
    protected open fun foo() = "OK"
}

class D : C() {
    // same package, but `super` needs to be related by class hierarchy:
    fun bar() = { super.foo() }
}

fun box() = D().bar()()
