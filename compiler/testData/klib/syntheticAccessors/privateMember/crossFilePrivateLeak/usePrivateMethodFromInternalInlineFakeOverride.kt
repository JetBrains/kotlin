// FILE: A.kt
open class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = privateMethod()
}

class B: A()

// FILE: B.kt
fun box(): String {
    return B().internalInlineMethod()
}
