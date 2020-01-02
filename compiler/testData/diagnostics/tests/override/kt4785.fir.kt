interface T {
    fun foo()
}

open class C {
    protected fun foo() {}
}

class E : C(), T

val z: T = object : C(), T {}
