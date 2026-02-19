// FILE: A.kt
class A {
    class Nested{
        private fun privateMethod() = "OK"

        internal inline fun internalInlineMethod() = privateMethod()
    }
}

// FILE: main.kt
fun box(): String {
    return A.Nested().internalInlineMethod()
}
