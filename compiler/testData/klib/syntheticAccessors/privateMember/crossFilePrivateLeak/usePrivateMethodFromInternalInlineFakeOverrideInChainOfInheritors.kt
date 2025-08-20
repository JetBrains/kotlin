// FILE: A.kt
open class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = privateMethod()
}

open class B: A()

class C: B()

// FILE: B.kt
fun box(): String {
    return C().internalInlineMethod()
}
