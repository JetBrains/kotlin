// IGNORE_BACKEND: JS_IR

// FILE: A.kt
class A {
    private fun privateMethod() = "OK"
    inner class Inner{
        internal inline fun internalMethod() = A().privateMethod()
    }
}

// FILE: main.kt
fun box(): String {
    return A().Inner().internalMethod()
}
