interface A {
    protected fun foo(): String

    fun box() = foo()
}

interface B : A

interface C : A {
    protected override fun foo() = "OK"
}

class D : B, C

fun box() = D().box()
