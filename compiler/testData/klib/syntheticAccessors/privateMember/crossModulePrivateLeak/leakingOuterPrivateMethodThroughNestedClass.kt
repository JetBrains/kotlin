// IGNORE_BACKEND: JS_IR

// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "OK"
    class Nested{
        internal inline fun internalInlineMethod() = A().privateMethod()
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A.Nested().internalInlineMethod()
}
