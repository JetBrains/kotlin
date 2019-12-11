interface T {
    public fun foo()
}

open class C {
    protected fun foo() {}
}

class D : C(), T

val obj: C = object : C(), T {}