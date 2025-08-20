open class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = privateMethod()
}

open class B: A()

class C: B()

fun box(): String {
    return C().internalInlineMethod()
}
