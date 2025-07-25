// ISSUE: KT-78960

open class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = privateMethod()
}

class B: A()

fun box(): String {
    return B().internalInlineMethod()
}
