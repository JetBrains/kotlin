// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    private fun privateMethod() = "OK"
    class Nested{
        internal inline fun internalInlineMethod() = A().privateMethod()
    }
}

// FILE: main.kt
fun box(): String {
    return A.Nested().internalInlineMethod()
}
